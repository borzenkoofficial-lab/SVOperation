package ru.svoperation.model;

import java.util.List;
import java.util.Map;

/** Immutable, triangulated Wavefront mesh and its parsed material library. */
public record ObjModel(List<Vertex> vertices, List<MeshPart> parts, Map<String, Material> materials) {
    public ObjModel {
        vertices = List.copyOf(vertices);
        parts = List.copyOf(parts);
        materials = Map.copyOf(materials);
    }

    public record Vertex(float x, float y, float z, float u, float v,
                         float nx, float ny, float nz) {}
    public record MeshPart(String name, String material, int firstVertex, int vertexCount) {}
    public record Material(float red, float green, float blue, float alpha, float shininess, String diffuseMap) {
        public int argb() {
            return (clamp(alpha) << 24) | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
        }
        private static int clamp(float value) { return Math.max(0, Math.min(255, Math.round(value * 255.0F))); }
    }
}
