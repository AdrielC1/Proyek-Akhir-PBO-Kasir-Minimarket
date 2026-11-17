
package com.siritel.exception;

public class StokTidakCukupException extends Exception {

    private static final long serialVersionUID = 1L;
    private final int sisaStok;

    // Constructor
    public StokTidakCukupException(String msg, int sisaStok) {
        super(msg);

        if (sisaStok < 0) {
            throw new IllegalArgumentException(
                "Sisa stok tidak boleh negatif."
            );
        }

        this.sisaStok = sisaStok;
    }

    // Getter
    public int getSisaStok() {
        return sisaStok;
    }
}
