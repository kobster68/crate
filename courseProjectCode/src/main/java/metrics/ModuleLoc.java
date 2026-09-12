/**
 * Copyright Michael A.
 */

package metrics;

public record ModuleLoc(String module, int files, int totalLines, int codeLines, int commentLines, int blankLines, double commentDensityPct) {

}
