package test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    ModelTest.class,
    CsvRepositoryTest.class,
    CustomRepositoryTest.class,
    DataGeneratorTest.class,
    ConcurrencyLockTest.class
})
public class TestModelJUnit {
    // Lớp rỗng để chạy JUnit Suite gom tất cả các lớp kiểm thử con
}
