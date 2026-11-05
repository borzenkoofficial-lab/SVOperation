package ru.svoperation.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dependency-free Wavefront OBJ/MTL reader. Faces are triangulated for GPU upload. */
public final class ObjModelLoader {
    private static final float UV_WORLD_SCALE = 1.0F / 2.5F;
    private ObjModelLoader() {}

    public static ObjModel parse(Reader source) throws IOException {
        return parse(source, null);
    }

    public static ObjModel parse(Reader source, Reader materialLibrary) throws IOException {
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<ObjModel.Vertex> output = new ArrayList<>();
        List<ObjModel.MeshPart> parts = new ArrayList<>();
        List<Ref> faceRefs = new ArrayList<>();
        String object = "default", material = "default";
        int partStart = 0;
        boolean uvDecisionMade = false;
        boolean generatePlanarUvs = false;
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
                            partStart = closePart(parts, object, material, partStart, output.size());
                            object = tokens.length > 1 ? tokens[1] : "unnamed";
                        }
                        case "usemtl" -> {
                            partStart = closePart(parts, object, material, partStart, output.size());
                            material = tokens.length > 1 ? tokens[1] : "default";
                        }
                        case "f" -> {
                            if (tokens.length < 4) throw new IllegalArgumentException("face has fewer than 3 vertices");
                            if (!uvDecisionMade) {
                                generatePlanarUvs = !hasUsableUvLayout(uvs);
                                uvDecisionMade = true;
                            }
                            faceRefs.clear();
                            for (int i = 1; i < tokens.length; i++) {
                                String[] refs = tokens[i].split("/", -1);
                                if (refs.length > 3 || refs[0].isEmpty()) throw new IllegalArgumentException("invalid face reference");
                                int p = index(refs[0], positions.size());
                                int t = refs.length > 1 && !refs[1].isEmpty() ? index(refs[1], uvs.size()) : -1;
                                int n = refs.length > 2 && !refs[2].isEmpty() ? index(refs[2], normals.size()) : -1;
                                faceRefs.add(new Ref(p, t, n));
                            }
                            float[] a = positions.get(faceRefs.get(0).position);
                            float[] b = positions.get(faceRefs.get(1).position);
                            float[] c = positions.get(faceRefs.get(2).position);
                            float[] faceNormal = cross(sub(b, a), sub(c, a));
                            normalize(faceNormal);
                            for (int i = 1; i < faceRefs.size() - 1; i++) {
                                emit(output, faceRefs.get(0), positions, uvs, normals, faceNormal, generatePlanarUvs);
                                emit(output, faceRefs.get(i), positions, uvs, normals, faceNormal, generatePlanarUvs);
                                emit(output, faceRefs.get(i + 1), positions, uvs, normals, faceNormal, generatePlanarUvs);
                            }
                        }
                        default -> { /* mtllib, smoothing groups and free-form surfaces are intentionally ignored */ }
                    }
                } catch (RuntimeException exception) {
                    throw new IOException("Invalid OBJ at line " + lineNumber + ": " + exception.getMessage(), exception);
                }
            }
        }
        closePart(parts, object, material, partStart, output.size());
        if (output.isEmpty()) throw new IOException("OBJ contains no renderable faces");
        Map<String, ObjModel.Material> materials = materialLibrary == null ? Map.of() : parseMaterials(materialLibrary);
        return new ObjModel(output, parts, materials);
    }

    private static boolean hasUsableUvLayout(List<float[]> uvs) {
        Set<String> unique = new HashSet<>();
        for (float[] uv : uvs) {
            unique.add(Float.floatToIntBits(uv[0]) + ":" + Float.floatToIntBits(uv[1]));
            if (unique.size() >= 16) return true;
        }
        return false;
    }

    public static Map<String, ObjModel.Material> parseMaterials(Reader source) throws IOException {
        Map<String, ObjModel.Material> materials = new LinkedHashMap<>();
        String name = null, diffuseMap = null;
        float r = 1, g = 1, b = 1, alpha = 1, shininess = 0;
        try (BufferedReader reader = new BufferedReader(source)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] t = trimmed.split("\\s+", 2);
                String value = t.length > 1 ? t[1].trim() : "";
                switch (t[0]) {
                    case "newmtl" -> {
                        if (name != null) materials.put(name, new ObjModel.Material(r,g,b,alpha,shininess,diffuseMap));
                        name = value; r = g = b = alpha = 1; shininess = 0; diffuseMap = null;
                    }
                    case "Kd" -> {
                        String[] rgb = value.split("\\s+");
                        if (rgb.length >= 3) { r = Float.parseFloat(rgb[0]); g = Float.parseFloat(rgb[1]); b = Float.parseFloat(rgb[2]); }
                    }
                    case "d" -> alpha = Float.parseFloat(value);
                    case "Tr" -> alpha = 1.0F - Float.parseFloat(value);
                    case "Ns" -> shininess = Float.parseFloat(value);
                    case "map_Kd" -> diffuseMap = value;
                    default -> { }
                }
            }
        } catch (RuntimeException exception) {
            throw new IOException("Invalid MTL material library: " + exception.getMessage(), exception);
        }
        if (name != null) materials.put(name, new ObjModel.Material(r,g,b,alpha,shininess,diffuseMap));
        return Map.copyOf(materials);
    }

    private static int closePart(List<ObjModel.MeshPart> parts, String name, String material, int start, int end) {
        if (end > start) parts.add(new ObjModel.MeshPart(name, material, start, end - start));
        return end;
    }
    private record Ref(int position, int uv, int normal) {}
    private static void emit(List<ObjModel.Vertex> out, Ref ref, List<float[]> p, List<float[]> uv,
                             List<float[]> n, float[] fallback, boolean generatePlanarUvs) {
        float[] pos = p.get(ref.position);
        float[] normal = ref.normal >= 0 ? n.get(ref.normal) : fallback;
        float[] tex = generatePlanarUvs ? planarUv(pos, normal)
                : (ref.uv >= 0 ? uv.get(ref.uv) : planarUv(pos, normal));
        out.add(new ObjModel.Vertex(pos[0], pos[1], pos[2], tex[0], 1 - tex[1],
                normal[0], normal[1], normal[2]));
    }

    /**
     * The original SV-01 mesh repeats only four UV corners on every polygon.
     * Generate a stable world-space planar projection instead of stretching the
     * complete diffuse image over each face. The 2.5-unit projection bounds cover
     * the authored weapon mesh without requiring texture coordinates in the OBJ.
     */
    private static float[] planarUv(float[] position, float[] normal) {
        float ax = Math.abs(normal[0]), ay = Math.abs(normal[1]), az = Math.abs(normal[2]);
        if (ax >= ay && ax >= az) {
            return new float[]{(position[2] + 1.25F) * UV_WORLD_SCALE,
                    (position[1] + 1.25F) * UV_WORLD_SCALE};
        }
        if (ay >= az) {
            return new float[]{(position[0] + 1.25F) * UV_WORLD_SCALE,
                    (position[2] + 1.25F) * UV_WORLD_SCALE};
        }
        return new float[]{(position[0] + 1.25F) * UV_WORLD_SCALE,
                (position[1] + 1.25F) * UV_WORLD_SCALE};
    }

    private static float[] vector(String[] t,int start,int size) {
        if(t.length<start+size) throw new IllegalArgumentException("incomplete vector");
        float[] v=new float[size]; for(int i=0;i<size;i++) v[i]=Float.parseFloat(t[start+i]); return v;
    }
    private static int index(String token,int size) {
        int value=Integer.parseInt(token), result=value>0?value-1:size+value;
        if(result<0||result>=size) throw new IllegalArgumentException("index out of range: "+token); return result;
    }
    private static float[] sub(float[] a,float[] b){return new float[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};}
    private static float[] cross(float[] a,float[] b){return new float[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    private static void normalize(float[] v){float l=(float)Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);if(l>1e-8f){v[0]/=l;v[1]/=l;v[2]/=l;}}
}
