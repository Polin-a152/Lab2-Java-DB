import java.util.HashMap;
import java.util.Random;

public class IndexBenchmark {
    public static void main(String[] args) {
        int count = 100000;
        int searches = 20000;

        int[] ids = new int[count];
        HashMap<Integer, Integer> index = new HashMap<Integer, Integer>();

        for (int i = 0; i < count; i++) {
            ids[i] = i;
            index.put(i, i);
        }

        Random random = new Random(10);
        int[] keys = new int[searches];
        for (int i = 0; i < searches; i++) {
            keys[i] = random.nextInt(count);
        }

        long start = System.nanoTime();
        long sumWithoutIndex = 0;
        for (int i = 0; i < searches; i++) {
            int key = keys[i];
            for (int j = 0; j < ids.length; j++) {
                if (ids[j] == key) {
                    sumWithoutIndex += ids[j];
                    break;
                }
            }
        }
        long timeWithoutIndex = System.nanoTime() - start;

        start = System.nanoTime();
        long sumWithIndex = 0;
        for (int i = 0; i < searches; i++) {
            Integer value = index.get(keys[i]);
            if (value != null) {
                sumWithIndex += value;
            }
        }
        long timeWithIndex = System.nanoTime() - start;

        System.out.println("Количество записей: " + count);
        System.out.println("Количество поисков: " + searches);
        System.out.println("Без индекса, мс: " + (timeWithoutIndex / 1000000.0));
        System.out.println("С индексом, мс: " + (timeWithIndex / 1000000.0));
        System.out.println("Контрольные суммы: " + sumWithoutIndex + " / " + sumWithIndex);
    }
}
