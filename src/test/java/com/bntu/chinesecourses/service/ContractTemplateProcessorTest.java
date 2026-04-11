package com.bntu.chinesecourses.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContractTemplateProcessorTest {

  private ContractTemplateProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new ContractTemplateProcessor();
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  /** Creates a minimal DOCX with one paragraph containing the given text in a single run. */
  private byte[] docxWithParagraph(String text) throws IOException {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      XWPFParagraph p = doc.createParagraph();
      XWPFRun run = p.createRun();
      run.setText(text);
      doc.write(out);
      return out.toByteArray();
    }
  }

  /** Creates a DOCX where a placeholder is split across three runs. */
  private byte[] docxWithSplitPlaceholder(String part1, String part2, String part3)
      throws IOException {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      XWPFParagraph p = doc.createParagraph();
      p.createRun().setText(part1);
      p.createRun().setText(part2);
      p.createRun().setText(part3);
      doc.write(out);
      return out.toByteArray();
    }
  }

  /** Reads the first paragraph text from a DOCX byte array. */
  private String firstParagraphText(byte[] docxBytes) throws IOException {
    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
      XWPFParagraph first = doc.getParagraphs().stream()
          .filter(p -> !p.getText().isBlank())
          .findFirst()
          .orElseThrow(() -> new AssertionError("No non-blank paragraph found"));
      return first.getText();
    }
  }

  /** Reads text from the first non-empty table cell of the first table. */
  private String firstTableCellText(byte[] docxBytes) throws IOException {
    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
      XWPFTable table = doc.getTables().get(0);
      XWPFTableRow row = table.getRows().get(0);
      return row.getTableCells().get(0).getText();
    }
  }

  // ── validation ────────────────────────────────────────────────────────────

  @Test
  void nullStream_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> processor.process(null, Map.of("__a__", "b")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("stream");
  }

  @Test
  void nullPlaceholders_throwsIllegalArgumentException() throws IOException {
    byte[] docx = docxWithParagraph("hello");
    assertThatThrownBy(() -> processor.process(new ByteArrayInputStream(docx), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Placeholders");
  }

  @Test
  void emptyPlaceholders_throwsIllegalArgumentException() throws IOException {
    byte[] docx = docxWithParagraph("hello");
    assertThatThrownBy(() -> processor.process(new ByteArrayInputStream(docx), Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Placeholders");
  }

  // ── single-run replacement ────────────────────────────────────────────────

  @Test
  void singlePlaceholder_isReplaced() throws IOException {
    byte[] docx = docxWithParagraph("Привет, __firstName__!");
    byte[] result = processor.process(
        new ByteArrayInputStream(docx),
        Map.of("__firstName__", "Иван"));

    assertThat(firstParagraphText(result)).isEqualTo("Привет, Иван!");
  }

  @Test
  void multiplePlaceholders_inOneParagraph_areAllReplaced() throws IOException {
    byte[] docx = docxWithParagraph("__lastName__ __firstName__ (__email__)");
    byte[] result = processor.process(
        new ByteArrayInputStream(docx),
        Map.of("__lastName__", "Иванов", "__firstName__", "Иван", "__email__", "i@x.com"));

    assertThat(firstParagraphText(result)).isEqualTo("Иванов Иван (i@x.com)");
  }

  @Test
  void unknownPlaceholder_isLeftUnchanged() throws IOException {
    byte[] docx = docxWithParagraph("Номер: __contractNumber__");
    byte[] result = processor.process(
        new ByteArrayInputStream(docx),
        Map.of("__firstName__", "Иван")); // no contractNumber entry

    assertThat(firstParagraphText(result)).isEqualTo("Номер: __contractNumber__");
  }

  @Test
  void paragraphWithNoPlaceholder_isUnchanged() throws IOException {
    byte[] docx = docxWithParagraph("Обычный текст без плейсхолдеров.");
    byte[] result = processor.process(
        new ByteArrayInputStream(docx),
        Map.of("__firstName__", "Иван"));

    assertThat(firstParagraphText(result)).isEqualTo("Обычный текст без плейсхолдеров.");
  }

  // ── split-run merging ─────────────────────────────────────────────────────

  @Test
  void placeholder_splitAcrossRuns_isReplacedCorrectly() throws IOException {
    // Word sometimes splits "__firstName__" as "__first" + "Name" + "__"
    byte[] docx = docxWithSplitPlaceholder("Имя: __first", "Name", "__!");
    byte[] result = processor.process(
        new ByteArrayInputStream(docx),
        Map.of("__firstName__", "Пётр"));

    assertThat(firstParagraphText(result)).isEqualTo("Имя: Пётр!");
  }

  @Test
  void splitPlaceholder_otherRunsAreCleared() throws IOException {
    byte[] docx = docxWithSplitPlaceholder("__", "firstName", "__");
    byte[] result = processor.process(
        new ByteArrayInputStream(docx),
        Map.of("__firstName__", "Анна"));

    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
      XWPFParagraph para = doc.getParagraphs().stream()
          .filter(p -> !p.getText().isBlank())
          .findFirst()
          .orElseThrow();
      // Only one run should contain text; the others should be empty
      long nonEmptyRuns = para.getRuns().stream()
          .filter(r -> r.getText(0) != null && !r.getText(0).isEmpty())
          .count();
      assertThat(nonEmptyRuns).isEqualTo(1);
      assertThat(para.getText()).isEqualTo("Анна");
    }
  }

  // ── table cells ───────────────────────────────────────────────────────────

  @Test
  void placeholder_inTableCell_isReplaced() throws IOException {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      XWPFTable table = doc.createTable(1, 1);
      table.getRow(0).getCell(0).getParagraphs().get(0)
          .createRun().setText("Слушатель: __fullName__");
      doc.write(out);
      byte[] docx = out.toByteArray();

      byte[] result = processor.process(
          new ByteArrayInputStream(docx),
          Map.of("__fullName__", "Иванов Иван Иванович"));

      assertThat(firstTableCellText(result)).isEqualTo("Слушатель: Иванов Иван Иванович");
    }
  }

  // ── replacePlaceholdersInParagraph (unit) ─────────────────────────────────

  @Test
  void replacePlaceholdersInParagraph_emptyRunList_doesNothing() throws IOException {
    try (XWPFDocument doc = new XWPFDocument()) {
      XWPFParagraph para = doc.createParagraph();
      // No runs added — should not throw
      processor.replacePlaceholdersInParagraph(para, Map.of("__x__", "y"));
      assertThat(para.getRuns()).isEmpty();
    }
  }

  @Test
  void replacePlaceholdersInParagraph_noMatchingPlaceholder_leavesRunsUnchanged()
      throws IOException {
    try (XWPFDocument doc = new XWPFDocument()) {
      XWPFParagraph para = doc.createParagraph();
      para.createRun().setText("Текст без плейсхолдера");
      processor.replacePlaceholdersInParagraph(para, Map.of("__name__", "Иван"));
      assertThat(para.getText()).isEqualTo("Текст без плейсхолдера");
    }
  }
}
