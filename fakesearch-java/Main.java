import java.util.*;
import java.util.concurrent.*;

class Result {
    String val;

    public Result(String v) {
        val = v;
    }

    @Override
    public String toString() {
        return val;
    }
}

class FakeSearch {
    String kind;

    public FakeSearch(String k) {
        kind = k;
    }

    public Result invoke(String query) throws InterruptedException {
        int rn = ThreadLocalRandom.current().nextInt(100);
        // System.out.println(kind + " is working for " + rn + " ms");
        Thread.sleep(rn);// some time consuming tasks...
        return new Result(String.format("%s-result-for:\"%s\"", kind, query));
    }
}

public class Main {

    public static FakeSearch Web = new FakeSearch("web");
    public static FakeSearch Image = new FakeSearch("image");
    public static FakeSearch Video = new FakeSearch("video");
    // replicas for V4
    public static FakeSearch Web1 = new FakeSearch("web");
    public static FakeSearch Image1 = new FakeSearch("image");
    public static FakeSearch Video1 = new FakeSearch("video");
    public static FakeSearch Web2 = new FakeSearch("web");
    public static FakeSearch Image2 = new FakeSearch("image");
    public static FakeSearch Video2 = new FakeSearch("video");

    // invokes Web, Image, and Video searches serially, appending them to the
    // results
    public static List<Result> GooglV1(String query) throws InterruptedException {

        List<Result> results = new ArrayList<>();
        results.add(Web.invoke(query));
        results.add(Image.invoke(query));
        results.add(Video.invoke(query));
        return results;

    }

    // Run the Web, Image, and Video searches concurrently, and wait for all results
    public static List<Result> GooglV2(String query) throws InterruptedException, ExecutionException {
        List<Result> results = new ArrayList<>();
        List<FakeSearch> fakeSearches = List.of(Web, Image, Video);

        ExecutorService pool = Executors.newFixedThreadPool(fakeSearches.size());
        List<Callable<Result>> fakeSearchTasks = new ArrayList<>();

        for (FakeSearch fakeSearch : fakeSearches) {
            fakeSearchTasks.add(() -> fakeSearch.invoke(query));
        }

        List<Future<Result>> futures = pool.invokeAll(fakeSearchTasks);

        for (Future<Result> futureResult : futures) {
            results.add(futureResult.get());
        }

        pool.shutdown(); // important...

        return results;
    }

    // Don't wait for slow servers...
    public static List<Result> GooglV3(String query)
            throws InterruptedException, ExecutionException, TimeoutException {
        List<Result> results = new ArrayList<>();
        List<FakeSearch> fakeSearches = List.of(Web, Image, Video);
        ExecutorService pool = Executors.newFixedThreadPool(fakeSearches.size());
        List<Callable<Result>> fakeSearchTasks = new ArrayList<>();

        for (FakeSearch fakeSearch : fakeSearches) {
            fakeSearchTasks.add(() -> fakeSearch.invoke(query));
        }

        List<Future<Result>> futures = pool.invokeAll(fakeSearchTasks, 80, TimeUnit.MILLISECONDS);

        for (Future<Result> futureResult : futures) {
            if (futureResult.isCancelled()) {
                results.add(new Result("timed-out"));
                pool.shutdown();
                return results;
            } else {
                results.add(futureResult.get());
            }
        }

        pool.shutdown();
        return results;
    }

    // avoid discarding results from slow servers; send requests to multiple
    // replicas, and use the first response.
    public static List<Result> GooglV4(String query) throws InterruptedException, ExecutionException {
        List<Result> results = new ArrayList<>();
        // these are replicated fakesearch objects
        List<Set<FakeSearch>> fakeSearches = List.of(Set.of(Web1, Web2), Set.of(Image1, Image2),
                Set.of(Video1, Video2));
        ExecutorService pool = Executors.newFixedThreadPool(fakeSearches.size());
        List<Callable<Result>> fakeSearchTasks = new ArrayList<>();

        for (Set<FakeSearch> fakeSearch : fakeSearches) {
            fakeSearchTasks.add(() -> First(query, fakeSearch));
        }

        List<Future<Result>> futures = pool.invokeAll(fakeSearchTasks);

        for (Future<Result> futureResult : futures) {
            results.add(futureResult.get());
        }

        pool.shutdown();
        return results;
    }

    private static Result First(String query, Set<FakeSearch> replicas)
            throws InterruptedException, ExecutionException, TimeoutException {
        var result = new Result("timed-out");

        var pool = Executors.newFixedThreadPool(replicas.size());
        var fakeSearchTasks = new ArrayList<Callable<Result>>();

        replicas.forEach((fakeSearch) -> {
            fakeSearchTasks.add(() -> fakeSearch.invoke(query));
        });

        // give me a winner, timeout logic not needed because that is the point of
        // picking a winner in replicas, someone will likely get a timely response
        result = pool.invokeAny(fakeSearchTasks);

        pool.shutdown();
        return result;
    }

    public static void RunAll() throws InterruptedException, ExecutionException, TimeoutException {
        int versions = 4;
        int iterations = 10;

        for (int v = 1; v <= versions; v++) {
            for (int i = 0; i < iterations; i++) {
                List<Result> results = new ArrayList<>();
                String searchQuery = "Java is great";
                long startTime = System.currentTimeMillis();
                switch (v) {
                    case 1:
                        results = GooglV1(searchQuery);
                        break;
                    case 2:
                        results = GooglV2(searchQuery);
                        break;
                    case 3:
                        results = GooglV3(searchQuery);
                        break;
                    case 4:
                        results = GooglV4(searchQuery);
                        break;
                    default:
                        System.err.println("Error, check version for search call");
                        break;
                }
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("Java results, V%d, %s, %d ms%n", v, results.toString().replace(",", ""), elapsed); 

            }
        }

        // for (int i = 0; i < iterations; i++) {
        // long startTime = System.currentTimeMillis();
        // List<Result> results = new ArrayList<>();
        // results = GooglV1("Java is great");
        // long elapsed = System.currentTimeMillis() - startTime;
        // System.out.printf("Java results, V1, %s, %sms%n", results, elapsed);

        // }
        // for (int i = 0; i < iterations; i++) {
        // long startTime = System.currentTimeMillis();
        // List<Result> results = new ArrayList<>();
        // results = GooglV2("Java is great");
        // long elapsed = System.currentTimeMillis() - startTime;
        // System.out.printf("Java results, V2, %s, %sms%n", results, elapsed);

        // }
        // for (int i = 0; i < iterations; i++) {
        // long startTime = System.currentTimeMillis();
        // List<Result> results = new ArrayList<>();
        // results = GooglV3("Java is great");
        // long elapsed = System.currentTimeMillis() - startTime;
        // System.out.printf("Java results, V3, %s, %sms%n", results, elapsed);

        // }
        // for (int i = 0; i < iterations; i++) {
        // long startTime = System.currentTimeMillis();
        // List<Result> results = new ArrayList<>();
        // results = GooglV4("Java is great");
        // long elapsed = System.currentTimeMillis() - startTime;
        // System.out.printf("Java results, V4, %s, %sms\n", results, elapsed);

        // }

    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        RunAll();
    }

}
