import java.util.ArrayList;
import java.util.HashSet;

public class SqlParser {
    private Database db;

    public SqlParser(Database db) {
        this.db = db;
    }
   
    public String execute(String sql) {
        sql = sql.trim();
        if (sql.length() == 0) {
            throw new IllegalArgumentException("Пустая команда");
        }

        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }

        String upper = sql.toUpperCase();

        if (upper.startsWith("INSERT INTO ")) {
            return insert(sql);
        }
        if (upper.startsWith("SELECT ")) {
            return select(sql);
        }
        if (upper.startsWith("UPDATE ")) {
            return update(sql);
        }
        if (upper.startsWith("DELETE FROM ")) {
            return delete(sql);
        }
        if (upper.startsWith("CREATE TABLE ")) {
            return createTable(sql);
        }
        if (upper.startsWith("DROP TABLE ")) {
            String tableName = sql.substring(10).trim();
            db.dropTable(tableName);
            return "Таблица удалена";
        }

        throw new IllegalArgumentException("Команда не поддерживается");
    }

    private String insert(String sql) {
        int valuesPosition = sql.toUpperCase().indexOf(" VALUES ");
        if (valuesPosition < 0) {
            throw new IllegalArgumentException("В команде INSERT отсутствует VALUES");
        }

        String left = sql.substring(12, valuesPosition).trim();
        String valuesText = sql.substring(valuesPosition + 8).trim();

        String tableName;
        ArrayList<String> insertColumns = new ArrayList<String>();

        int open = left.indexOf('(');
        if (open >= 0) {
            int close = left.lastIndexOf(')');
            if (close < open) {
                throw new IllegalArgumentException("Неверный список столбцов");
            }
            tableName = left.substring(0, open).trim();
            insertColumns = split(left.substring(open + 1, close));
        } else {
            tableName = left;
        }

        Table table = db.getTable(tableName);
        ArrayList<Integer> columnNumbers = new ArrayList<Integer>();

        if (insertColumns.size() == 0) {
            for (int i = 0; i < table.getColumns().size(); i++) {
                columnNumbers.add(i);
            }
        } else {
            for (int i = 0; i < insertColumns.size(); i++) {
                int number = table.columnNumber(insertColumns.get(i));
                if (number < 0) {
                    throw new IllegalArgumentException("Нет столбца: " + insertColumns.get(i));
                }
                if (columnNumbers.contains(number)) {
                    throw new IllegalArgumentException("Столбец указан два раза");
                }
                columnNumbers.add(number);
            }
        }

        ArrayList<String> groups = readValueGroups(valuesText);
        ArrayList<Row> newRows = new ArrayList<Row>();
        HashSet<Object> newKeys = new HashSet<Object>();
        int keyNumber = table.primaryKeyNumber();

        for (int i = 0; i < groups.size(); i++) {
            ArrayList<String> values = split(groups.get(i));
            if (values.size() != columnNumbers.size()) {
                throw new IllegalArgumentException("Количество значений не совпадает со столбцами");
            }

            ArrayList<Object> rowValues = new ArrayList<Object>();
            for (int j = 0; j < table.getColumns().size(); j++) {
                rowValues.add(null);
            }

            for (int j = 0; j < values.size(); j++) {
                int number = columnNumbers.get(j);
                Object value = table.getColumns().get(number).convert(values.get(j));
                rowValues.set(number, value);
            }

            Row row = new Row(rowValues);
            if (keyNumber >= 0) {
                Object key = row.get(keyNumber);
                if (key == null) {
                    throw new IllegalArgumentException("Первичный ключ не может быть NULL");
                }
                if (table.findByPrimaryKey(key) != null) {
                    throw new IllegalArgumentException("Такой первичный ключ уже существует: " + key);
                }
                if (!newKeys.add(key)) {
                    throw new IllegalArgumentException("Такой первичный ключ уже существует: " + key);
                }
            }
            db.validateInsert(tableName, row);
            newRows.add(row);
        }

        
        for (int i = 0; i < newRows.size(); i++) {
            db.insert(tableName, newRows.get(i));
        }

        return "Добавлено записей: " + newRows.size();
    }

    private ArrayList<String> readValueGroups(String text) {
        ArrayList<String> groups = new ArrayList<String>();
        int depth = 0;
        boolean quotes = false;
        int start = -1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\'') {
                if (quotes && i + 1 < text.length() && text.charAt(i + 1) == '\'') {
                    i++;
                    continue;
                }
                quotes = !quotes;
            }

            if (!quotes) {
                if (c == '(') {
                    if (depth == 0) {
                        start = i + 1;
                    }
                    depth++;
                } else if (c == ')') {
                    depth--;
                    if (depth < 0) {
                        throw new IllegalArgumentException("Неверные скобки в VALUES");
                    }
                    if (depth == 0) {
                        groups.add(text.substring(start, i));
                    }
                } else if (c == ',' && depth == 0 && !text.substring(i + 1).trim().isEmpty()) {
                    
                }
            }
        }

        if (quotes) {
            throw new IllegalArgumentException("Неверный формат VALUES");
        }
        if (depth != 0) {
            throw new IllegalArgumentException("Неверный формат VALUES");
        }
        if (groups.size() == 0) {
            throw new IllegalArgumentException("Неверный формат VALUES");
        }
        return groups;
    }

    private String select(String sql) {
        int from = sql.toUpperCase().indexOf(" FROM ");
        if (from < 0) {
            throw new IllegalArgumentException("В SELECT отсутствует FROM");
        }

        ArrayList<String> names = split(sql.substring(7, from).trim());
        String rest = sql.substring(from + 6).trim();
        int where = rest.toUpperCase().indexOf(" WHERE ");
        String tableName = where < 0 ? rest : rest.substring(0, where).trim();
        Table table = db.getTable(tableName);
        ArrayList<Row> found = new ArrayList<Row>();

        if (where < 0) {
            found.addAll(table.getRows());
        } else {
            String[] condition = condition(rest.substring(where + 7).trim());
            int column = table.columnNumber(condition[0]);
            if (column < 0) {
                throw new IllegalArgumentException("Нет столбца: " + condition[0]);
            }
            Object wanted = table.getColumns().get(column).convert(condition[2]);

            
            if (column == table.primaryKeyNumber() && condition[1].equals("=")) {
                Row row = table.findByPrimaryKey(wanted);
                if (row != null) {
                    found.add(row);
                }
            } else {
                for (int i = 0; i < table.getRows().size(); i++) {
                    Row row = table.getRows().get(i);
                    if (compare(row.get(column), wanted, condition[1])) {
                        found.add(row);
                    }
                }
            }
        }

        return table.select(names, found);
    }

    private String update(String sql) {
        int set = sql.toUpperCase().indexOf(" SET ");
        int where = sql.toUpperCase().indexOf(" WHERE ");
        if (set < 0) {
            throw new IllegalArgumentException("Формат UPDATE: UPDATE table SET column=value WHERE condition");
        }
        if (where < 0) {
            throw new IllegalArgumentException("Формат UPDATE: UPDATE table SET column=value WHERE condition");
        }
        if (where < set) {
            throw new IllegalArgumentException("Формат UPDATE: UPDATE table SET column=value WHERE condition");
        }

        String tableName = sql.substring(7, set).trim();
        String[] change = sql.substring(set + 5, where).split("=", 2);
        if (change.length != 2) {
            throw new IllegalArgumentException("Неверное присваивание в SET");
        }

        Table table = db.getTable(tableName);
        int changeColumn = table.columnNumber(change[0].trim());
        if (changeColumn < 0) {
            throw new IllegalArgumentException("Нет столбца: " + change[0].trim());
        }
        Object newValue = table.getColumns().get(changeColumn).convert(change[1]);

        String[] cond = condition(sql.substring(where + 7).trim());
        int conditionColumn = table.columnNumber(cond[0]);
        if (conditionColumn < 0) {
            throw new IllegalArgumentException("Нет столбца: " + cond[0]);
        }
        Object wanted = table.getColumns().get(conditionColumn).convert(cond[2]);

        ArrayList<Row> selected = new ArrayList<Row>();
        for (int i = 0; i < table.getRows().size(); i++) {
            Row row = table.getRows().get(i);
            if (compare(row.get(conditionColumn), wanted, cond[1])) {
                selected.add(row);
            }
        }

        
        int keyNumber = table.primaryKeyNumber();
        if (changeColumn == keyNumber && keyNumber >= 0 && selected.size() > 0) {
            if (newValue == null) {
                throw new IllegalArgumentException("Первичный ключ не может быть NULL");
            }
            if (selected.size() > 1) {
                throw new IllegalArgumentException("Нельзя установить одинаковый первичный ключ нескольким строкам");
            }
            Row existing = table.findByPrimaryKey(newValue);
            if (existing != null && !selected.contains(existing)) {
                throw new IllegalArgumentException("Такой первичный ключ уже существует");
            }
        }

        db.checkUpdate(table, changeColumn, newValue);

        
        if (changeColumn == keyNumber && keyNumber >= 0 && selected.size() > 0) {
            for (int i = 0; i < selected.size(); i++) {
                db.checkDelete(table, selected.get(i));
            }
        }

        for (int i = 0; i < selected.size(); i++) {
            table.updateValue(selected.get(i), changeColumn, newValue);
        }

        return "Изменено записей: " + selected.size();
    }

    private String delete(String sql) {
        int where = sql.toUpperCase().indexOf(" WHERE ");
        if (where < 0) {
            throw new IllegalArgumentException("Для DELETE требуется WHERE");
        }

        String tableName = sql.substring(12, where).trim();
        Table table = db.getTable(tableName);
        String[] cond = condition(sql.substring(where + 7).trim());
        int column = table.columnNumber(cond[0]);
        if (column < 0) {
            throw new IllegalArgumentException("Нет столбца: " + cond[0]);
        }
        Object wanted = table.getColumns().get(column).convert(cond[2]);

        ArrayList<Row> selected = new ArrayList<Row>();
        for (int i = 0; i < table.getRows().size(); i++) {
            Row row = table.getRows().get(i);
            if (compare(row.get(column), wanted, cond[1])) {
                selected.add(row);
            }
        }

        
        for (int i = 0; i < selected.size(); i++) {
            db.checkDelete(table, selected.get(i));
        }
        for (int i = 0; i < selected.size(); i++) {
            table.remove(selected.get(i));
        }

        return "Удалено записей: " + selected.size();
    }

    private String createTable(String sql) {
        int open = sql.indexOf('(');
        int close = sql.lastIndexOf(')');
        if (open < 0) {
            throw new IllegalArgumentException("Неверный CREATE TABLE");
        }
        if (close < open) {
            throw new IllegalArgumentException("Неверный CREATE TABLE");
        }

        String tableName = sql.substring(12, open).trim();
        ArrayList<String> definitions = split(sql.substring(open + 1, close));
        ArrayList<Column> columns = new ArrayList<Column>();

        for (int i = 0; i < definitions.size(); i++) {
            String definition = definitions.get(i).trim();
            String[] words = definition.split("\\s+");
            if (words.length < 2) {
                throw new IllegalArgumentException("Неверное описание столбца");
            }

            boolean primary = false;
            String referenceTable = null;
            String referenceColumn = null;

            for (int j = 2; j < words.length; j++) {
                if (words[j].equalsIgnoreCase("PRIMARY") && j + 1 < words.length
                        && words[j + 1].equalsIgnoreCase("KEY")) {
                    primary = true;
                }
                if (words[j].equalsIgnoreCase("REFERENCES") && j + 1 < words.length) {
                    String reference = words[j + 1];
                    int refOpen = reference.indexOf('(');
                    int refClose = reference.indexOf(')');
                    if (refOpen > 0 && refClose > refOpen) {
                        referenceTable = reference.substring(0, refOpen);
                        referenceColumn = reference.substring(refOpen + 1, refClose);
                    }
                }
            }
            columns.add(new Column(words[0], words[1], primary,
                    referenceTable, referenceColumn));
        }

        db.createTable(tableName, columns);
        return "Таблица создана: " + tableName;
    }

    private ArrayList<String> split(String text) {
        ArrayList<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean quotes = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'') {
                if (quotes && i + 1 < text.length() && text.charAt(i + 1) == '\'') {
                    current.append("''");
                    i++;
                    continue;
                }
                quotes = !quotes;
            }

            if (c == ',' && !quotes) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (quotes) {
            throw new IllegalArgumentException("Не закрыта строка в кавычках");
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        } else if (text.trim().length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private String[] condition(String text) {
        String[] operators = {">=", "<=", "!=", "<>", "=", ">", "<"};
        for (int i = 0; i < operators.length; i++) {
            int position = text.indexOf(operators[i]);
            if (position >= 0) {
                String column = text.substring(0, position).trim();
                String value = text.substring(position + operators[i].length()).trim();
                if (column.length() == 0) {
                    break;
                }
                if (value.length() == 0) {
                    break;
                }
                return new String[] {column, operators[i], value};
            }
        }
        throw new IllegalArgumentException("Неверное условие WHERE");
    }

    private boolean compare(Object a, Object b, String operator) {
        if (a == null) {
            return operator.equals("=") && a == null && b == null;
        }
        if (b == null) {
            return operator.equals("=") && a == null && b == null;
        }

        int result;
        if (a instanceof Number && b instanceof Number) {
            result = Double.compare(((Number) a).doubleValue(),
                    ((Number) b).doubleValue());
        } else if (a instanceof Comparable && a.getClass().equals(b.getClass())) {
            result = ((Comparable<Object>) a).compareTo(b);
        } else {
            result = a.toString().compareTo(b.toString());
        }

        if (operator.equals("=")) return result == 0;
        if (operator.equals("!=")) return result != 0;
        if (operator.equals("<>")) return result != 0;
        if (operator.equals(">")) return result > 0;
        if (operator.equals("<")) return result < 0;
        if (operator.equals(">=")) return result >= 0;
        if (operator.equals("<=")) return result <= 0;
        return false;
    }
}
