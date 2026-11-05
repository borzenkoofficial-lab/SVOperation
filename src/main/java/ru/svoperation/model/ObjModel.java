package ru.svoperation.model;

import java.util.List;

/** Immutable, renderer-independent representation of a triangulated Wavefront OBJ mesh. */
public record ObjModel(List<Vertex> vertices, List<MeshPart> parts) {
    public ObjModel {
        vertices = List.copyOf(vertices);
        parts = List.copyOf(parts);
    }

    public record Vertex(float x, float y, float z, float u, float v,
                         float nx, float ny, float nz) {}

    public record MeshPart(String name, String material, int firstVertex, int vertexCount) {}
}
