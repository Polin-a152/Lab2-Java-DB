import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Table implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private ArrayList<Column> columns;
    private ArrayList<Row> rows;
    private HashMap<Object, Row> primaryKeyIndex;

    public Table(String name, ArrayList<Column> columns) {
        this.name = name.toLowerCase();
        this.columns = columns;
        this.rows = new ArrayList<Row>();
        this.primaryKeyIndex = new HashMap<Object, Row>();
    }

    public String getName() {
        return name;
    }

    public ArrayList<Column> getColumns() {
        return columns;
    }

    public ArrayList<Row> getRows() {
        return rows;
    }

    public int columnNumber(String columnName) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    public int primaryKeyNumber() {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).isPrimaryKey()) {
                return i;
            }
        }
        return -1;
    }

    public Row findByPrimaryKey(Object key) {
        if (primaryKeyNumber() < 0) {
            return null;
        }
        return primaryKeyIndex.get(key);
    }

    public void insert(Row row) {
        int keyNumber = primaryKeyNumber();

        if (keyNumber >= 0) {
            Object key = row.get(keyNumber);
            if (key == null) {
                throw new IllegalArgumentException("Первичный ключ не может быть NULL");
            }
            if (primaryKeyIndex.containsKey(key)) {
                throw new IllegalArgumentException("Такой первичный ключ уже существует");
            }
            primaryKeyIndex.put(key, row);
        }

        rows.add(row);
    }

    public void remove(Row row) {
        int keyNumber = primaryKeyNumber();
        if (keyNumber >= 0) {
            primaryKeyIndex.remove(row.get(keyNumber));
        }
        Iterator<Row> iterator = rows.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == row) {
                iterator.remove();
                break;
            }
        }
    }

    public void updateValue(Row row, int columnNumber, Object newValue) {
        int keyNumber = primaryKeyNumber();

        if (columnNumber == keyNumber && keyNumber >= 0) {
            Object oldKey = row.get(keyNumber);
            primaryKeyIndex.remove(oldKey);
            row.set(columnNumber, newValue);
            primaryKeyIndex.put(newValue, row);
        } else {
            row.set(columnNumber, newValue);
        }
    }

    public void rebuildIndex() {
        primaryKeyIndex = new HashMap<Object, Row>();
        int keyNumber = primaryKeyNumber();

        if (keyNumber >= 0) {
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                Object key = row.get(keyNumber);
                if (key != null) {
                    primaryKeyIndex.put(key, row);
                }
            }
        }
    }

    public String select(ArrayList<String> names, ArrayList<Row> found) {
        ArrayList<Integer> numbers = new ArrayList<Integer>();

        if (names.size() == 1 && names.get(0).equals("*")) {
            for (int i = 0; i < columns.size(); i++) {
                numbers.add(i);
            }
        } else {
            for (int i = 0; i < names.size(); i++) {
                int number = columnNumber(names.get(i));
                if (number < 0) {
                    throw new IllegalArgumentException("Нет столбца: " + names.get(i));
                }
                numbers.add(number);
            }
        }

        int[] widths = new int[numbers.size()];
        for (int i = 0; i < numbers.size(); i++) {
            widths[i] = columns.get(numbers.get(i)).getName().length();
        }

        for (int i = 0; i < found.size(); i++) {
            for (int j = 0; j < numbers.size(); j++) {
                String value = String.valueOf(found.get(i).get(numbers.get(j)));
                if (value.length() > widths[j]) {
                    widths[j] = value.length();
                }
            }
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < numbers.size(); i++) {
            result.append(pad(columns.get(numbers.get(i)).getName(), widths[i])).append(" | ");
        }
        result.append("\n");

        for (int i = 0; i < numbers.size(); i++) {
            for (int j = 0; j < widths[i]; j++) {
                result.append("-");
            }
            result.append("-+-");
        }
        result.append("\n");

        for (int i = 0; i < found.size(); i++) {
            for (int j = 0; j < numbers.size(); j++) {
                String value = String.valueOf(found.get(i).get(numbers.get(j)));
                result.append(pad(value, widths[j])).append(" | ");
            }
            result.append("\n");
        }

        result.append("Количество записей: ").append(found.size());
        return result.toString();
    }

    private String pad(String value, int width) {
        StringBuilder result = new StringBuilder(value);
        while (result.length() < width) {
            result.append(" ");
        }
        return result.toString();
    }
}
