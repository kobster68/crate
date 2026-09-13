/**
 * Copyright Michael A. 
 */

package metrics;

public record FileMetrics(FileData fileData, FileLoc fileLoc) {
    
    public String module() {
        return fileData.module();
    }
    
    
    
    
}
