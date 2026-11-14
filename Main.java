import java.util.Scanner;

public class Main {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Storage storage = new Storage("data/database.ser");
        Database db;

        if (storage.exists()) {
            try {
                db = storage.load();
                System.out.println("База данных загружена.");
            } catch (Exception e) {
                System.out.println("Не удалось прочитать файл базы. Создана новая база.");
                db = Database.createDefault();
            }
        } else {
            db = Database.createDefault();
            System.out.println("Создана новая база данных.");
        }

        SqlParser parser = new SqlParser(db);
        boolean work = true;

        while (work) {
            System.out.println();
            System.out.println("1. Выполнить SQL-запрос");
            System.out.println("2. Показать таблицы");
            System.out.println("3. Выход");
            System.out.print("Выберите пункт: ");

            if (!scanner.hasNextLine()) {
                break;
            }
            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                System.out.print("SQL> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String sql = scanner.nextLine();

                try {
                    String result = parser.execute(sql);
                    System.out.println(result);
                    storage.save(db);
                } catch (Exception e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            } else if (choice.equals("2")) {
                System.out.println("Таблицы: " + db.getTableNames());
            } else if (choice.equals("3")) {
                try {
                    storage.save(db);
                    System.out.println("Данные сохранены.");
                } catch (Exception e) {
                    System.out.println("Ошибка сохранения: " + e.getMessage());
                }
                work = false;
            } else {
                System.out.println("Нет такого пункта.");
            }
        }

        scanner.close();
    }
}
