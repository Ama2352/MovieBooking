# Maven Surefire Configuration for Test Tagging

Để chạy tests theo tags, thêm configuration sau vào `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.0.0</version>
            <configuration>
                <!-- Enable JUnit 5 Platform -->
                <properties>
                    <configurationParameters>
                        junit.jupiter.execution.parallel.enabled = true
                        junit.jupiter.execution.parallel.mode.default = concurrent
                    </configurationParameters>
                </properties>
                
                <!-- Test Groups Configuration -->
                <groups>${test.groups}</groups>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Maven Profiles cho từng loại test

Thêm vào `pom.xml`:

```xml
<profiles>
    <!-- Smoke Test Profile -->
    <profile>
        <id>smoke</id>
        <properties>
            <test.groups>smoke</test.groups>
        </properties>
    </profile>
    
    <!-- Sanity Test Profile -->
    <profile>
        <id>sanity</id>
        <properties>
            <test.groups>sanity</test.groups>
        </properties>
    </profile>
    
    <!-- Regression Test Profile -->
    <profile>
        <id>regression</id>
        <properties>
            <test.groups>regression</test.groups>
        </properties>
    </profile>
    
    <!-- All Tests (default) -->
    <profile>
        <id>all-tests</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <test.groups>smoke | sanity | regression</test.groups>
        </properties>
    </profile>
</profiles>
```

## Cách sử dụng

### Option 1: Với profiles
```bash
# Chạy smoke tests
mvn test -P smoke

# Chạy sanity tests
mvn test -P sanity

# Chạy regression tests
mvn test -P regression

# Chạy tất cả tests
mvn test -P all-tests
```

### Option 2: Với groups parameter
```bash
# Chạy smoke tests
mvn test -Dgroups="smoke"

# Chạy sanity tests
mvn test -Dgroups="sanity"

# Chạy regression tests
mvn test -Dgroups="regression"

# Chạy nhiều groups
mvn test -Dgroups="smoke | sanity"

# Exclude một group
mvn test -Dgroups="!regression"
```

### Option 3: Combine profiles với parameters
```bash
# Chạy smoke + sanity
mvn test -P smoke,sanity

# Chạy tất cả trừ regression
mvn test -Dgroups="smoke | sanity"
```

## JUnit Platform Console

Nếu muốn chạy từ command line với JUnit Console Launcher:

```bash
java -jar junit-platform-console-standalone.jar \
  --include-tag smoke \
  --scan-classpath
```

## GitHub Actions Integration

Thêm vào `.github/workflows/test.yml`:

```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  smoke-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run Smoke Tests
        run: mvn test -P smoke
        
  sanity-tests:
    runs-on: ubuntu-latest
    needs: smoke-tests
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run Sanity Tests
        run: mvn test -P sanity
        
  regression-tests:
    runs-on: ubuntu-latest
    needs: sanity-tests
    if: github.event_name == 'schedule' || github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run Regression Tests
        run: mvn test -P regression
```

## IntelliJ IDEA Configuration

1. **Create Run Configurations**:
   - Run → Edit Configurations
   - Add New → JUnit
   - Name: "Smoke Tests"
   - Test kind: Tags
   - Tag expression: `smoke`
   
2. **Repeat cho Sanity và Regression**

3. **Sử dụng**: Run → Run 'Smoke Tests'

## Dependencies Required

Đảm bảo có các dependencies sau trong `pom.xml`:

```xml
<dependencies>
    <!-- JUnit 5 Platform -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- JUnit Platform Suite -->
    <dependency>
        <groupId>org.junit.platform</groupId>
        <artifactId>junit-platform-suite</artifactId>
        <version>1.10.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- JUnit Platform Launcher -->
    <dependency>
        <groupId>org.junit.platform</groupId>
        <artifactId>junit-platform-launcher</artifactId>
        <version>1.10.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Troubleshooting

### Issue: Tags không được nhận diện
**Solution**: Kiểm tra xem annotation có đúng package không
```java
import com.api.moviebooking.tags.SmokeTest;
```

### Issue: Maven không chạy tests với tags
**Solution**: Thêm `maven-surefire-plugin` version 3.0.0+

### Issue: IntelliJ không hiển thị tests với tags
**Solution**: 
1. File → Invalidate Caches / Restart
2. Rebuild project
3. Đảm bảo JUnit 5 được enable

## Report Generation

Tạo test reports với tags:

```bash
# Generate test report
mvn test -P smoke
mvn surefire-report:report

# View report at: target/site/surefire-report.html
```

## Best Practices

1. ✅ Luôn chạy smoke tests trước khi commit
2. ✅ Chạy sanity tests sau khi fix bug
3. ✅ Chạy regression tests trước release
4. ✅ Update tags khi thêm/sửa tests
5. ✅ Document lý do chọn tag trong test comment
