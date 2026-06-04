package model;

/**
 * BaseEntity - Abstract class cho tat ca thuc the.
 * Yeu cau tu PDF: Implement abstract BaseEntity + toCsvLine/fromCsvLine.
 */
public abstract class BaseEntity {
    
    // Tra ve Id de dung lam key trong Repository
    public abstract String getId();

    // Chuyen doi Entity thanh dong CSV
    public abstract String toCsvLine();

    // Kien truc chuan yeu cau moi class con phai co phuong thuc fromCsvLine, 
    // nhung trong Java abstract khong the ep buoc static method, 
    // nen viec parse se do Repository hoac tung class con tu handle.
}

