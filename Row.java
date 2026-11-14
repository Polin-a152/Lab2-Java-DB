import java.io.Serializable;
import java.util.ArrayList;

public class Row implements Serializable {
    private static final long serialVersionUID = 1L;

    private ArrayList<Object> values;

    public Row(ArrayList<Object> values) {
        this.values = values;
    }

    public Object get(int index) {
        return values.get(index);
    }

    public void set(int index, Object value) {
        values.set(index, value);
    }

    public int size() {
        return values.size();
    }
}
