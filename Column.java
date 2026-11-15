import java.io.Serializable
import java.time.LocalDate;

public class Column implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String type;
    private boolean primaryKey;
    private String referenceTable;
    private String referenceColumn;

    public Column(String name, String type, boolean primaryKey) {
        this(name, type, primaryKey, null, null);
    }

    public Column(String name, String type, boolean primaryKey,
                  String referenceTable, String referenceColumn) {
        this.name = name.toLowerCase();
        this.type = type.toUpperCase();
        this.primaryKey = primaryKey;
        this.referenceTable = referenceTable;
        this.referenceColumn = referenceColumn;
    }
    public String getName() {
        return name;
    }
    public String getType() {
        return type;
    }
    public boolean isPrimaryKey() {
        return primaryKey;
    }
    public String getReferenceTable() {
        return referenceTable;
    }
    public String getReferenceColumn() {
        return referenceColumn;
    }
    public Object convert(String value) {
        value = value.trim();

        if (value.equalsIgnoreCase("NULL")) {
            return null;
        }
        if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
            value = value.replace("''", "'");
        }
        if (type.equals("INT")) {
            return Integer.valueOf(value);
        }
        if (type.equals("DOUBLE")) {
            return Double.valueOf(value);
        }
        if (type.equals("STRING")) {
            return value;
        }
        if (type.equals("DATE")) {
            return LocalDate.parse(value);
        }
        if (type.equals("BOOLEAN")) {
            if (value.equalsIgnoreCase("TRUE")) {
                return Boolean.TRUE;
            }
            if (value.equalsIgnoreCase("FALSE")) {
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Для BOOLEAN используйте TRUE или FALSE");
        }

        throw new IllegalArgumentException("Неизвестный тип данных: " + type);
    }
}
