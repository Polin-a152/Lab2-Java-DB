import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

public class Database implements Serializable {
    private static final long serialVersionUID = 1L;

    private java.util.HashMap<String, Table> tables =
            new java.util.HashMap<String, Table>();

    public static Database createDefault() {
        Database db = new Database();

        ArrayList<Column> cars = new ArrayList<Column>();
        cars.add(new Column("id", "INT", true));
        cars.add(new Column("brand", "STRING", false));
        cars.add(new Column("price", "DOUBLE", false));
        cars.add(new Column("available", "BOOLEAN", false));
        db.createTable("cars", cars);

        ArrayList<Column> owners = new ArrayList<Column>();
        owners.add(new Column("id", "INT", true));
        owners.add(new Column("name", "STRING", false));
        owners.add(new Column("car_id", "INT", false, "cars", "id"));
        owners.add(new Column("purchase_date", "DATE", false));
        db.createTable("owners", owners);

        return db;
    }
  
    public void createTable(String name, ArrayList<Column> columns) {
        name = name.toLowerCase();
        if (tables.containsKey(name)) {
            throw new IllegalArgumentException("Таблица уже существует: " + name);
        }
        if (columns.size() == 0) {
            throw new IllegalArgumentException("В таблице должен быть хотя бы один столбец");
        }

        int primaryKeys = 0;
        HashSet<String> names = new HashSet<String>();
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            if (!names.add(column.getName())) {
                throw new IllegalArgumentException("Повторяется имя столбца: " + column.getName());
            }
            if (column.isPrimaryKey()) {
                primaryKeys++;
            }
            if (!isSupportedType(column.getType())) {
                throw new IllegalArgumentException("Неизвестный тип: " + column.getType());
            }
        }
        if (primaryKeys > 1) {
            throw new IllegalArgumentException("Поддерживается один первичный ключ");
        }

        Table table = new Table(name, columns);
        tables.put(name, table);
    }

    private boolean isSupportedType(String type) {
        if (type.equals("INT")) return true;
        if (type.equals("DOUBLE")) return true;
        if (type.equals("STRING")) return true;
        if (type.equals("DATE")) return true;
        if (type.equals("BOOLEAN")) return true;
        return false;
    }

    public Table getTable(String name) {
        Table table = tables.get(name.toLowerCase());
        if (table == null) {
            throw new IllegalArgumentException("Таблица не найдена: " + name);
        }
        return table;
    }
    public ArrayList<String> getTableNames() {
        return new ArrayList<String>(tables.keySet());
    }
    public void dropTable(String name) {
        name = name.toLowerCase();
        getTable(name);

        for (Table table : tables.values()) {
            for (Column column : table.getColumns()) {
                if (name.equals(column.getReferenceTable())) {
                    throw new IllegalArgumentException(
                            "Нельзя удалить таблицу: на нее ссылается " + table.getName());
                }
            }
        }
        tables.remove(name);
    }
    public void validateInsert(String tableName, Row row) {
        Table table = getTable(tableName);

        if (row.size() != table.getColumns().size()) {
            throw new IllegalArgumentException("Неверное количество значений");
        }

        for (int i = 0; i < table.getColumns().size(); i++) {
            Column column = table.getColumns().get(i);
            if (column.getReferenceTable() != null && row.get(i) != null) {
                Table parent = getTable(column.getReferenceTable());
                int keyNumber = parent.columnNumber(column.getReferenceColumn());
                boolean found = false;

                for (int j = 0; j < parent.getRows().size(); j++) {
                    if (row.get(i).equals(parent.getRows().get(j).get(keyNumber))) {
                        found = true;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException("Не найдена запись для внешнего ключа");
                }
            }
        }
    }

    public void insert(String tableName, Row row) {
        validateInsert(tableName, row);
        getTable(tableName).insert(row);
    }
    public void checkUpdate(Table table, int columnNumber, Object value) {
        Column column = table.getColumns().get(columnNumber);
        if (column.getReferenceTable() != null && value != null) {
            Table parent = getTable(column.getReferenceTable());
            int keyNumber = parent.columnNumber(column.getReferenceColumn());
            if (parent.findByPrimaryKey(value) == null) {
                boolean found = false;
                for (int i = 0; i < parent.getRows().size(); i++) {
                    if (value.equals(parent.getRows().get(i).get(keyNumber))) {
                        found = true;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException("Не найдена запись для внешнего ключа");
                }
            }
        }
    }
    public void checkDelete(Table table, Row row) {
        int keyNumber = table.primaryKeyNumber();
        if (keyNumber < 0) {
            return;
        }

        Object value = row.get(keyNumber);
        for (Table child : tables.values()) {
            for (int i = 0; i < child.getColumns().size(); i++) {
                Column column = child.getColumns().get(i);
                if (table.getName().equals(column.getReferenceTable())) {
                    for (int j = 0; j < child.getRows().size(); j++) {
                        if (value.equals(child.getRows().get(j).get(i))) {
                            throw new IllegalArgumentException(
                                    "Сначала удалите связанную запись из " + child.getName());
                        }
                    }
                }
            }
        }
    }
    public void rebuildIndexes() {
        for (Table table : tables.values()) {
            table.rebuildIndex();
        }
    }
}
