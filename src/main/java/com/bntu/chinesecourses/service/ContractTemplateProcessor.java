package com.bntu.chinesecourses.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

/**
 * Processes DOCX templates by replacing {@code __placeholder__} occurrences with provided values.
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>Placeholders follow the {@code __camelCase__} convention (letters only between the
 *       underscores).
 *   <li>Word sometimes splits a single logical token across several {@link XWPFRun} objects.
 *       This processor merges all runs in a paragraph into a single string, performs
 *       replacements, then writes the result back into the first run and clears the others.
 *       The first run's font/style is preserved; style differences within the same paragraph
 *       may be lost — acceptable for administrative contract templates.
 *   <li>Unknown placeholders (not present in the supplied map) are left unchanged.
 *   <li>Both body paragraphs and table-cell paragraphs are processed.
 * </ul>
 */
@Component
public class ContractTemplateProcessor {

  static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("__[a-zA-Z]+__");

  /**
   * Processes a DOCX template stream and returns the patched document bytes.
   *
   * @param templateStream input stream of the source {@code .docx} file (closed by this method)
   * @param placeholders   map of {@code __key__} → replacement value; must not be null or empty
   * @return patched DOCX bytes
   * @throws IllegalArgumentException if arguments are invalid
   * @throws IllegalStateException    if the template cannot be parsed or written
   */
  public byte[] process(InputStream templateStream, Map<String, String> placeholders) {
    if (templateStream == null) {
      throw new IllegalArgumentException("Template stream must not be null");
    }
    if (placeholders == null || placeholders.isEmpty()) {
      throw new IllegalArgumentException("Placeholders map must not be null or empty");
    }

    try (XWPFDocument document = new XWPFDocument(templateStream);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      // Body paragraphs
      for (XWPFParagraph paragraph : document.getParagraphs()) {
        replacePlaceholdersInParagraph(paragraph, placeholders);
      }

      // Table-cell paragraphs
      for (XWPFTable table : document.getTables()) {
        for (XWPFTableRow row : table.getRows()) {
          for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
              replacePlaceholdersInParagraph(paragraph, placeholders);
            }
          }
        }
      }

      document.write(out);
      return out.toByteArray();

    } catch (IOException e) {
      throw new IllegalStateException("Failed to process contract template", e);
    }
  }

  /**
   * Replaces all known placeholders within a single paragraph.
   *
   * <p>The method merges run texts, performs replacements on the combined string, then writes
   * the result back only when at least one replacement was made.
   */
  void replacePlaceholdersInParagraph(XWPFParagraph paragraph, Map<String, String> placeholders) {
    List<XWPFRun> runs = paragraph.getRuns();
    if (runs == null || runs.isEmpty()) {
      return;
    }

    // Build combined text from all runs
    StringBuilder combined = new StringBuilder();
    for (XWPFRun run : runs) {
      String text = run.getText(0);
      if (text != null) {
        combined.append(text);
      }
    }

    String original = combined.toString();
    if (!PLACEHOLDER_PATTERN.matcher(original).find()) {
      return; // No placeholders in this paragraph — skip
    }

    String replaced = original;
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      replaced = replaced.replace(entry.getKey(), entry.getValue());
    }

    if (replaced.equals(original)) {
      return; // No match found — leave as-is
    }

    // Write replaced text into the first run; clear the rest
    runs.get(0).setText(replaced, 0);
    for (int i = 1; i < runs.size(); i++) {
      runs.get(i).setText("", 0);
    }
  }
}
