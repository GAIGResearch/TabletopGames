package games.descent2e.pcg_clean.spatial;

import java.util.List;

/** One physical cardboard piece, with one or more mutually exclusive printed faces. */
public record PhysicalPiece(String id, List<String> faces) {
    public PhysicalPiece {
        faces = List.copyOf(faces);
        if (faces.isEmpty()) throw new IllegalArgumentException("A physical piece needs a face");
    }
}
