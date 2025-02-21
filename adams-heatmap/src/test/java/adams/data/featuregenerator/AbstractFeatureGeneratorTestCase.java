/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * AbstractFeatureGeneratorTestCase.java
 * Copyright (C) 2013-2015 University of Waikato, Hamilton, New Zealand
 */
package adams.data.featuregenerator;

import adams.core.CleanUpHandler;
import adams.core.Destroyable;
import adams.core.io.FileUtils;
import adams.core.option.OptionHandler;
import adams.core.option.OptionUtils;
import adams.data.heatmap.Heatmap;
import adams.data.io.input.AbstractHeatmapReader;
import adams.data.report.Report;
import adams.test.AbstractDatabaseTestCase;
import adams.test.AbstractTestHelper;
import adams.test.Regression;
import adams.test.TestHelper;
import adams.test.TmpFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Ancestor for heatmap feature generator test cases.
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public abstract class AbstractFeatureGeneratorTestCase
  extends AbstractDatabaseTestCase {

  /**
   * Constructs the test case. Called by subclasses.
   *
   * @param name 	the name of the test
   */
  public AbstractFeatureGeneratorTestCase(String name) {
    super(name);
  }

  /**
   * Returns the test helper class to use.
   *
   * @return		the helper class instance
   */
  @Override
  protected AbstractTestHelper newTestHelper() {
    return new TestHelper(this, "adams/data/featuregenerator/data");
  }

  /**
   * Loads the data to process.
   *
   * @param filename	the filename to load (without path)
   * @param reader	the reader to use
   * @return		the data, null if it could not be loaded
   */
  protected List<Heatmap> load(String filename, AbstractHeatmapReader reader) {
    List<Heatmap>	result;

    m_TestHelper.copyResourceToTmp(filename);
    reader.setInput(new TmpFile(filename));
    result = reader.read();
    m_TestHelper.deleteFileFromTmp(filename);

    return result;
  }
  
  /**
   * Processes the data.
   * 
   * @param generator	the generator to use
   * @param data	the input data
   * @return		the report(s) with the "additional" features
   */
  protected List<Report> process(AbstractFeatureGenerator generator, List<Heatmap> data) {
    List<Report>	result;
    Report		backup;

    result = new ArrayList<Report>();
    
    for (Heatmap d: data) {
      backup = d.getReport().getClone();
      d      = generator.generate(d);
      result.add(d.getReport().minus(backup));
    }
    
    return result;
  }

  /**
   * Saves the data in the tmp directory.
   *
   * @param data	the data to save
   * @param filename	the filename to save to (without path)
   * @param ignored	the indices of lines to ignore
   * @return		true if successfully saved
   */
  protected boolean save(List<Report> data, String filename, int[] ignored) {
    StringBuilder	output;
    int			i;
    List<String>	lines;
    int			n;

    output = new StringBuilder();
    for (i = 0; i < data.size(); i++) {
      output.append("#" + (i+1) + ":\n");
      lines = Arrays.asList(data.get(i).toString().split("\n"));
      lines = Regression.trim(lines, ignored);
      for (n = 0; n < lines.size(); n++)
	output.append(lines.get(n) + "\n");
      output.append("\n");
    }

    return FileUtils.writeToFile(new TmpFile(filename).getAbsolutePath(), output, false);
  }

  /**
   * Returns the database connection props files.
   * <br><br>
   * The default returns null.
   *
   * @return		the props files, null if to use the default one
   * @see		#getDatabasePropertiesFile()
   */
  protected String[] getRegressionConnections() {
    return null;
  }

  /**
   * Returns the filenames (without path) of the input data files to use
   * in the regression test.
   *
   * @return		the filenames
   */
  protected abstract String[] getRegressionInputFiles();

  /**
   * Returns the readers to use on the corresponding input files.
   *
   * @return		the readers
   * @see		#getRegressionInputFiles()
   */
  protected abstract AbstractHeatmapReader[] getRegressionInputFileReaders();

  /**
   * Returns the setups to use in the regression test.
   *
   * @return		the setups
   */
  protected abstract AbstractFeatureGenerator[] getRegressionSetups();

  /**
   * Returns the ignored line indices to use in the regression test.
   *
   * @return		the setups
   */
  protected int[] getRegressionIgnoredLineIndices() {
    return new int[0];
  }

  /**
   * Creates an output filename based on the input filename.
   *
   * @param input	the input filename (no path)
   * @param no		the number of the test
   * @return		the generated output filename (no path)
   */
  protected String createOutputFilename(String input, int no) {
    String	result;
    int		index;
    String	ext;

    ext = "-out" + no;

    index = input.lastIndexOf('.');
    if (index == -1) {
      result = input + ext;
    }
    else {
      result  = input.substring(0, index);
      result += ext;
      result += input.substring(index);
    }

    return result;
  }

  /**
   * Compares the processed data against previously saved output data.
   */
  public void testRegression() {
    List<Report>		processed;
    List<Heatmap>		data;
    boolean			ok;
    String			regression;
    int				i;
    String[]			input;
    AbstractFeatureGenerator[]	setups;
    AbstractFeatureGenerator	current;
    AbstractHeatmapReader[]	readers;
    String[]			output;
    TmpFile[]			outputFiles;
    int[]			ignored;
    String[]			props;

    if (m_NoRegressionTest)
      return;

    input   = getRegressionInputFiles();
    readers = getRegressionInputFileReaders();
    output  = new String[input.length];
    setups  = getRegressionSetups();
    ignored = getRegressionIgnoredLineIndices();
    props   = getRegressionConnections();
    assertEquals("Number of files and readers differ!", input.length, readers.length);
    assertEquals("Number of files and setups differ!", input.length, setups.length);
    if (props != null) {
      assertEquals("Number of files and connection setups differ!", input.length, props.length);
    }
    else {
      props = new String[input.length];
      for (i = 0; i < props.length; i++)
	props[i] = getDatabasePropertiesFile();
    }

    // process data
    for (i = 0; i < input.length; i++) {
      // connect to correct database
      reconnect(props[i]);

      current = (AbstractFeatureGenerator) OptionUtils.shallowCopy((OptionHandler) setups[i], false);
      assertNotNull("Failed to create copy of algorithm: " + OptionUtils.getCommandLine(setups[i]), current);

      data = load(input[i], readers[i]);
      assertNotNull("Failed to read data?", data);

      processed = process(current, data);
      assertNotNull("Failed to process data?", processed);
      
      output[i] = createOutputFilename(input[i], i);
      ok        = save(processed, output[i], ignored);
      assertTrue("Failed to save regression data?", ok);

      if (current instanceof Destroyable)
	((Destroyable) current).destroy();
    }

    // test regression
    outputFiles = new TmpFile[output.length];
    for (i = 0; i < output.length; i++)
      outputFiles[i] = new TmpFile(output[i]);
    regression = m_Regression.compare(outputFiles);
    assertNull("Output differs:\n" + regression, regression);

    // remove output, clean up scheme
    for (i = 0; i < output.length; i++) {
      if (setups[i] instanceof Destroyable)
	((Destroyable) setups[i]).destroy();
      else if (setups[i] instanceof CleanUpHandler)
	((CleanUpHandler) setups[i]).cleanUp();
      m_TestHelper.deleteFileFromTmp(output[i]);
    }
    cleanUpAfterRegression();

    // connect to default database
    m_Properties = null;
    getDatabaseProperties();
  }

  /**
   * For further cleaning up after the regression tests.
   * <br><br>
   * Default implementation does nothing.
   */
  protected void cleanUpAfterRegression() {
  }
}
