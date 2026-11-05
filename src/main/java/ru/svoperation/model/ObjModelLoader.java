package ru.svoperation.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Small dependency-free Wavefront OBJ reader for item meshes.
 * Supports v/vt/vn, object/group names, usemtl, positive and negative indices,
 * and polygon fan triangulation. MTL parsing and GPU upload are renderer concerns.
 */
public final class ObjModelLoader {
    private ObjModelLoader() {}

    public static ObjModel parse(Reader source) throws IOException {
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<ObjModel.Vertex> output = new ArrayList<>();
        List<ObjModel.MeshPart> parts = new ArrayList<>();
        String object = "default";
        String material = "default";
        int partStart = 0;

        try (BufferedReader reader = new BufferedReader(source)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] tokens = trimmed.split("\\s+");
                try {
                    switch (tokens[0]) {
                        case "v" -> positions.add(vector(tokens, 1, 3));
                        case "vt" -> uvs.add(vector(tokens, 1, 2));
                        case "vn" -> normals.add(vector(tokens, 1, 3));
                        case "o", "g" -> {
                            String next = tokens.length > 1 ? tokens[1] : "unnamed";
                            if (output.size() > partStart) {
                                parts.add(new ObjModel.MeshPart(object, material, partStart, output.size() - partStart));
                                partStart = output.size();
                            }
                            object = next;
                        }
                        case "usemtl" -> {
                            String next = tokens.length > 1 ? tokens[1] : "default";
                            if (output.size() > partStart) {
                                parts.add(new ObjModel.MeshPart(object, material, partStart, output.size() - partStart));
                                partStart = output.size();
                            }
                            material = next;
                        }
                        case "f" -> {
                            if (tokens.length < 4) throw new IllegalArgumentException("face has fewer than 3 vertices");
                            int[] face = new int[tokens.length - 1];
                            for (int i = 1; i < tokens.length; i++) {
                                String[] refs = tokens[i].split("/", -1);
                                int p = index(refs[0], positions.size());
                                int t = refs.length > 1 && !refs[1].isEmpty() ? index(refs[1], uvs.size()) : -1;
                                int n = refs.length > 2 && !refs[2].isEmpty() ? index(refs[2], normals.size()) : -1;
                                face[i - 1] = output.size();
                                // Store references temporarily; expanded after face normal is known.
                                faceRefs.add(new Ref(p, t, n));
                            }
                            Ref a = faceRefs.get(faceRefs.size() - face.length);
                            Ref b = faceRefs.get(faceRefs.size() - face.length + 1);
                            Ref c = faceRefs.get(faceRefs.size() - 1);
                            float[] pa = positions.get(a.position), pb = positions.get(b.position), pc = positions.get(c.position);
                            float[] cross = cross(sub(pb, pa), sub(pc, pa));
                            normalize(cross);
                            int base = faceRefs.size() - face.length;
                            for (int i = 1; i < face.length - 1; i++) {
                                emit(output, faceRefs.get(base), positions, uvs, normals, cross);
                                emit(output, faceRefs.get(base + i), positions, uvs, normals, cross);
                                emit(output, faceRefs.get(base + i + 1), positions, uvs, normals, cross);
                            }
                            faceRefs.subList(base, faceRefs.size()).clear();
                        }
                        default -> { /* mtllib, smoothing and free-form directives are intentionally ignored */ }
                    }
                } catch (RuntimeException exception) {
                    throw new IOException("Invalid OBJ at line " + lineNumber + ": " + exception.getMessage(), exception);
                }
            }
        }
        if (output.size() > partStart) {
            parts.add(new ObjModel.MeshPart(object, material, partStart, output.size() - partStart));
        }
        if (output.isEmpty()) throw new IOException("OBJ contains no renderable faces");
        return new ObjModel(output, parts);
    }

    private record Ref(int position, int uv, int normal) {}
    private static final List<Ref> faceRefs = new ArrayList<>();

    private static void emit(List<ObjModel.Vertex> out, Ref ref, List<float[]> p,
                             List<float[]> uv, List<float[]> n, float[] fallback) {
        float[] pos = p.get(ref.position);
        float[] tex = ref.uv >= 0 ? uv.get(ref.uv) : new float[]{0, 0};
        float[] normal = ref.normal >= 0 ? n.get(ref.normal) : fallback;
        out.add(new ObjModel.Vertex(pos[0], pos[1], pos[2], tex[0], 1 - tex[1],
                normal[0], normal[1], normal[2]));
    }

    private static float[] vector(String[] tokens, int start, int size) {
        if (tokens.length < start + size) throw new IllegalArgumentException("incomplete vector");
        float[] result = new float[size];
        for (int i = 0; i < size; i++) result[i] = Float.parseFloat(tokens[start + i]);
        return result;
    }

    private static int index(String token, int size) {
        int value = Integer.parseInt(token);
        int result = value > 0 ? value - 1 : size + value;
        if (result < 0 || result >= size) throw new IllegalArgumentException("index out of range: " + token);
        return result;
    }

    private static float[] sub(float[] a, float[] b) { return new float[]{a[0]-b[0], a[1]-b[1], a[2]-b[2]}; }
    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]};
    }
    private static void normalize(float[] v) {
        float length = (float)Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
        if (length > 1.0e-8f) { v[0]/=length; v[1]/=length; v[2]/=length; }
    }
}
