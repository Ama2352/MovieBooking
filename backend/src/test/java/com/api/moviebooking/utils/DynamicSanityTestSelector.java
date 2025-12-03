package com.api.moviebooking.utils;

import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dynamic Sanity Test Selector
 * Analyzes Git changes and selects relevant tests based on module mapping
 */
public class DynamicSanityTestSelector {
    
    private static final String MAPPING_FILE = "test-config/module-test-mapping.yml";
    private Map<String, ModuleConfig> moduleMapping;
    
    public DynamicSanityTestSelector() {
        loadModuleMapping();
    }
    
    @SuppressWarnings("unchecked")
    private void loadModuleMapping() {
        Yaml yaml = new Yaml();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream(MAPPING_FILE)) {
            if (in == null) {
                throw new RuntimeException("❌ Mapping file not found: " + MAPPING_FILE);
            }
            
            Map<String, Object> data = yaml.load(in);
            Map<String, Object> modules = (Map<String, Object>) data.get("modules");
            
            this.moduleMapping = new HashMap<>();
            for (Map.Entry<String, Object> entry : modules.entrySet()) {
                String moduleName = entry.getKey();
                Map<String, Object> config = (Map<String, Object>) entry.getValue();
                
                ModuleConfig moduleConfig = new ModuleConfig(
                    moduleName,
                    (String) config.get("description"),
                    (List<String>) config.get("source_patterns"),
                    (List<String>) config.get("test_classes"),
                    (List<String>) config.getOrDefault("test_tags", Collections.emptyList()),
                    (List<String>) config.getOrDefault("dependent_modules", Collections.emptyList())
                );
                
                this.moduleMapping.put(moduleName, moduleConfig);
            }
            
            System.out.println("✅ Loaded " + moduleMapping.size() + " module mappings");
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to load module mapping: " + e.getMessage(), e);
        }
    }
    
    /**
     * Select test classes based on changed files
     */
    public Set<String> selectTestClasses(List<String> changedFiles) {
        Set<String> testClasses = new HashSet<>();
        Set<String> affectedModules = findAffectedModules(changedFiles);
        
        if (affectedModules.isEmpty()) {
            return testClasses;
        }
        
        System.out.println("🔍 Affected modules: " + affectedModules);
        
        // Add direct module tests
        for (String moduleName : affectedModules) {
            ModuleConfig config = moduleMapping.get(moduleName);
            testClasses.addAll(config.getTestClasses());
            
            // Add dependent module tests
            for (String dependent : config.getDependentModules()) {
                ModuleConfig depConfig = moduleMapping.get(dependent);
                if (depConfig != null) {
                    System.out.println("  ↳ Including dependent module: " + dependent);
                    testClasses.addAll(depConfig.getTestClasses());
                }
            }
        }
        
        return testClasses;
    }
    
    /**
     * Select test tags based on changed files
     */
    public Set<String> selectTestTags(List<String> changedFiles) {
        Set<String> testTags = new HashSet<>();
        Set<String> affectedModules = findAffectedModules(changedFiles);
        
        if (affectedModules.isEmpty()) {
            return testTags;
        }
        
        for (String moduleName : affectedModules) {
            ModuleConfig config = moduleMapping.get(moduleName);
            testTags.addAll(config.getTestTags());
            
            // Add dependent module tags
            for (String dependent : config.getDependentModules()) {
                ModuleConfig depConfig = moduleMapping.get(dependent);
                if (depConfig != null) {
                    testTags.addAll(depConfig.getTestTags());
                }
            }
        }
        
        return testTags;
    }
    
    private Set<String> findAffectedModules(List<String> changedFiles) {
        Set<String> affectedModules = new HashSet<>();
        
        for (String file : changedFiles) {
            for (Map.Entry<String, ModuleConfig> entry : moduleMapping.entrySet()) {
                if (entry.getValue().matchesSourcePattern(file)) {
                    affectedModules.add(entry.getKey());
                    System.out.println("  📝 " + file + " → " + entry.getKey());
                }
            }
        }
        
        return affectedModules;
    }
    
    /**
     * Get changed files from Git (HEAD~1 to HEAD)
     */
    public List<String> getChangedFilesSinceLastCommit() {
        return getChangedFiles("HEAD~1", "HEAD");
    }
    
    /**
     * Get changed files between two Git refs
     */
    public List<String> getChangedFiles(String fromRef, String toRef) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "git", "diff", "--name-only", fromRef, toRef
            );
            pb.directory(new File(System.getProperty("user.dir")));
            Process process = pb.start();
            
            List<String> changedFiles;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                changedFiles = reader.lines()
                        .filter(line -> line.endsWith(".java"))
                        .collect(Collectors.toList());
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String errors = errorReader.lines().collect(Collectors.joining("\n"));
                    System.err.println("⚠️ Git command failed: " + errors);
                }
            }
            
            return changedFiles;
        } catch (Exception e) {
            System.err.println("⚠️ Failed to get Git changes: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Main method for CLI usage
     */
    public static void main(String[] args) {
        System.out.println("🚀 Dynamic Sanity Test Selector");
        System.out.println("================================\n");
        
        DynamicSanityTestSelector selector = new DynamicSanityTestSelector();
        
        List<String> changedFiles;
        if (args.length > 0) {
            // Use provided file list
            changedFiles = Arrays.asList(args);
            System.out.println("📂 Using provided file list");
        } else {
            // Auto-detect from Git
            System.out.println("🔍 Auto-detecting changes from Git...");
            changedFiles = selector.getChangedFilesSinceLastCommit();
        }
        
        if (changedFiles.isEmpty()) {
            System.out.println("⚠️ No Java files changed. Running smoke tests only.\n");
            System.out.println("--- CI/CD Output ---");
            System.out.println("TEST_CLASSES=");
            System.out.println("TEST_TAGS=SmokeTest");
            return;
        }
        
        System.out.println("\n📝 Changed files:");
        changedFiles.forEach(f -> System.out.println("  - " + f));
        System.out.println();
        
        Set<String> testClasses = selector.selectTestClasses(changedFiles);
        Set<String> testTags = selector.selectTestTags(changedFiles);
        
        System.out.println("\n🧪 Selected test classes (" + testClasses.size() + "):");
        testClasses.forEach(t -> System.out.println("  - " + t));
        
        System.out.println("\n🏷️ Selected test tags (" + testTags.size() + "):");
        testTags.forEach(t -> System.out.println("  - " + t));
        
        // Output for CI/CD (environment variables format)
        System.out.println("\n--- CI/CD Output ---");
        System.out.println("TEST_CLASSES=" + String.join(",", testClasses));
        System.out.println("TEST_TAGS=" + String.join(",", testTags));
    }
    
    /**
     * Module Configuration
     */
    static class ModuleConfig {
        private final String name;
        private final String description;
        private final List<String> sourcePatterns;
        private final List<String> testClasses;
        private final List<String> testTags;
        private final List<String> dependentModules;
        
        public ModuleConfig(String name, String description, 
                           List<String> sourcePatterns, 
                           List<String> testClasses,
                           List<String> testTags,
                           List<String> dependentModules) {
            this.name = name;
            this.description = description;
            this.sourcePatterns = sourcePatterns != null ? sourcePatterns : Collections.emptyList();
            this.testClasses = testClasses != null ? testClasses : Collections.emptyList();
            this.testTags = testTags != null ? testTags : Collections.emptyList();
            this.dependentModules = dependentModules != null ? dependentModules : Collections.emptyList();
        }
        
        public boolean matchesSourcePattern(String filePath) {
            // Normalize path separators
            String normalizedPath = filePath.replace('\\', '/');
            
            return sourcePatterns.stream()
                    .anyMatch(pattern -> matchPattern(normalizedPath, pattern));
        }
        
        private boolean matchPattern(String filePath, String pattern) {
            // Convert glob pattern to regex
            String regex = pattern
                    .replace(".", "\\.")
                    .replace("**/", ".*")
                    .replace("**", ".*")
                    .replace("*", "[^/]*");
            
            // Make it match anywhere in the path
            if (!regex.startsWith(".*")) {
                regex = ".*" + regex;
            }
            if (!regex.endsWith(".*")) {
                regex = regex + ".*";
            }
            
            return filePath.matches(regex);
        }
        
        public List<String> getTestClasses() { return testClasses; }
        public List<String> getTestTags() { return testTags; }
        public List<String> getDependentModules() { return dependentModules; }
    }
}
