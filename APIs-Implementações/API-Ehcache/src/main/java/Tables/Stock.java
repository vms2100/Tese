package Tables;


public class Stock {

    private  Integer ID_Book;
    private  Integer Qty;
    private  Integer ID_Library;

    public Stock() {
        this.ID_Book = ID_Book;
        this.ID_Library= ID_Library;
        this.Qty=Qty;
    }


    public void setIdBook(Integer ID_Book) {
        this.ID_Book=ID_Book;
    }
    public void setIdLibrary(Integer ID_Library) {
        this.ID_Library=ID_Library;
    }
    public void setQty(Integer qty) {
        this.Qty=qty;
    }

    public Integer getQty() {
        return Qty;
    }

    public Integer getID_Library() {
        return ID_Library;
    }

    public Integer getID_Book() {
        return ID_Book;
    }
}