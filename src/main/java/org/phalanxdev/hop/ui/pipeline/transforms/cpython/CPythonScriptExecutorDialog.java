/*! ******************************************************************************
 *
 * CPython for the Hop orchestration platform
 *
 * http://www.project-hop.org
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.phalanxdev.hop.ui.pipeline.transforms.cpython;

import org.apache.hop.core.Const;
import org.apache.hop.core.Props;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopPluginException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransformDialog;
import org.apache.hop.pipeline.transform.stream.IStream;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.ShowMessageDialog;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.ScriptStyledTextComp;
import org.apache.hop.ui.core.widget.StyledTextComp;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.core.widget.TextComposite;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.apache.hop.ui.util.EnvironmentUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.List;

import org.phalanxdev.hop.pipeline.transforms.cpython.CPythonScriptExecutorMeta;
import org.phalanxdev.python.PythonSession;

/**
 * Dialog for the CPythonScriptExecutor step
 *
 * @author Mark Hall (mhall{[at]}phalanxdev{[dot]}com)
 */
public class CPythonScriptExecutorDialog extends BaseTransformDialog implements ITransformDialog {

  private static Class<?> PKG = CPythonScriptExecutorMeta.class;

  private CTabFolder wctfContainer;

  private CTabItem wctiConfig, wctiScript, wctiFields;
  private Composite wcConfig, wcScript, wcFields;
  private SelectionAdapter lsDef;

  /**
   * Configure tab
   */
  private Group wgRowHandling, wgOptions;
  //row handling group
  private Label wlRowsToProcess, wlRowsToProcessSize, wlReservoirSampling, wlReservoirSamplingSize, wlRandomSeed;
  private ComboVar wcvRowsToProcess;
  private TextVar wtvRowsToProcessSize, wtvReservoirSamplingSize, wtvRandomSeed;
  private Button wbReservoirSampling;
  //options group
  private Label wlIncludeInputAsOutput, wlContinueOnUnsetVars, wlPythonCommand, wlPyPathEntries, wlPyServerID;
  private Button wbIncludeInputAsOutput, wbContinueOnUnsetVars;
  private TextVar wtvPythonCommand, wtvPyPathEntries, wtvPyServerID;
  //table
  private TableView wtvInputFrames;

  /**
   * Script tab
   */
  private Label wlLoadScriptFile, wlScriptLocation, wlScript;
  private Button wbLoadScriptFile, wbScriptBrowse;
  private TextVar wtvScriptLocation;
  private TextComposite wstcScriptEditor;

  /**
   * Fields tab
   */
  private Label wlPyVarsToGet;
  private TextVar wtvPyVarsToGet;
  private Label wlOutputFields;
  private TableView wtvOutputFields;
  private Button wbVarsToFields;
  private Button wbGetFields;
  private Button wbIncludeRowIndex;

  private FormData fd;
  private Control lastControl;

  //constants
  private static final int FIRST_LABEL_RIGHT_PERCENTAGE = 35;
  private static final int FIRST_PROMPT_RIGHT_PERCENTAGE = 55;
  private static final int SECOND_LABEL_RIGHT_PERCENTAGE = 65;
  private static final int SECOND_PROMPT_RIGHT_PERCENTAGE = 80;
  private static final int MARGIN = Const.MARGIN;
  private static int MIDDLE;

  protected CPythonScriptExecutorMeta m_inputMeta;
  protected CPythonScriptExecutorMeta m_originalMeta;

  //listeners
  ModifyListener simpleModifyListener = new ModifyListener() {
    @Override public void modifyText( ModifyEvent e ) {
      m_inputMeta.setChanged();
    }
  };

  SelectionAdapter simpleSelectionAdapter = new SelectionAdapter() {
    @Override public void widgetDefaultSelected( SelectionEvent e ) {
      ok();
    }
  };

  public CPythonScriptExecutorDialog( Shell parent, IVariables variables, Object inMeta, PipelineMeta tr, String sname ) {
    super( parent, variables, (BaseTransformMeta) inMeta, tr, sname );

    m_inputMeta = (CPythonScriptExecutorMeta) inMeta;
    m_originalMeta = (CPythonScriptExecutorMeta) m_inputMeta.clone();
  }

  public CPythonScriptExecutorDialog(Shell parent, IVariables variables, BaseTransformMeta baseTransformMeta,
      PipelineMeta pipelineMeta, String transformName) {
    super(parent, variables, baseTransformMeta, pipelineMeta, transformName);
    m_inputMeta = (CPythonScriptExecutorMeta) baseTransformMeta;
    m_originalMeta = (CPythonScriptExecutorMeta) m_inputMeta.clone();
  }

  public CPythonScriptExecutorDialog(Shell parent, int nr, IVariables variables, Object in, PipelineMeta tr ) {
    super(parent, nr, variables, (BaseTransformMeta) in, tr);

    m_inputMeta = (CPythonScriptExecutorMeta) in;
    m_originalMeta = (CPythonScriptExecutorMeta) m_inputMeta.clone();
  }

  @Override public String open() {

    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
    props.setLook( shell );
    setShellImage( shell, m_inputMeta );

    changed = m_inputMeta.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    shell.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.Shell.Title" ) ); //$NON-NLS-1$

    MIDDLE = props.getMiddlePct();

    // Stepname line
    wlTransformName = new Label( shell, SWT.RIGHT );
    wlTransformName
        .setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.Stepname.Label" ) ); //$NON-NLS-1$
    props.setLook( wlTransformName );
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment( 0, 0 );
    fdlTransformName.right = new FormAttachment( MIDDLE, -MARGIN );
    fdlTransformName.top = new FormAttachment( 0, MARGIN );
    wlTransformName.setLayoutData( fdlTransformName );
    wTransformName = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wTransformName.setText( transformName );
    props.setLook( wTransformName );
    wTransformName.addModifyListener( simpleModifyListener );
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment( MIDDLE, 0 );
    fdTransformName.top = new FormAttachment( 0, MARGIN );
    fdTransformName.right = new FormAttachment( 100, 0 );
    wTransformName.setLayoutData( fdTransformName );
    lastControl = wTransformName;

    wctfContainer = new CTabFolder( shell, SWT.BORDER );
    props.setLook( wctfContainer, Props.WIDGET_STYLE_TAB );
    if (!EnvironmentUtils.getInstance().isWeb()) {
      wctfContainer.setSimple(false);
    }

    addConfigureTab();
    addScriptTab();
    addFieldsTab();
    // checkPython();

    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( wTransformName, MARGIN );
    fd.right = new FormAttachment( 100, 0 );
    fd.bottom = new FormAttachment( 100, -50 );
    wctfContainer.setLayoutData( fd );

    // some buttons
    wOk = new Button( shell, SWT.PUSH );
    wOk.setText( BaseMessages.getString( PKG, "System.Button.OK" ) ); //$NON-NLS-1$
    wOk.addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( Event e ) {
        ok();
      }
    } );
    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) ); //$NON-NLS-1$
    wCancel.addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( Event e ) {
        cancel();
      }
    } );
    setButtonPositions( new Button[] { wOk, wCancel }, MARGIN, null );

    lsDef = new SelectionAdapter() {
      @Override public void widgetDefaultSelected( SelectionEvent e ) {
        ok();
      }
    };

    wTransformName.addSelectionListener( lsDef );

    // Detect X or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      @Override public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );

    getData( m_inputMeta );

    m_inputMeta.setChanged( changed );

    wctfContainer.setSelection( 0 );
    // Set the shell size, based upon previous time...
    setSize();

    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return transformName;
  }

  private void checkPython() {
    if ( !PythonSession.pythonAvailable() ) {
      // try initializing
      try {
        if ( !PythonSession.initSession( "python", variables, log ) ) {
          String envEvalResults = PythonSession.getPythonEnvCheckResults();
          logError(
              "Was unable to start the python environment:\n\n" + ( envEvalResults != null ? envEvalResults : "" ) );
        }
      } catch ( HopException ex ) {
        logError( "Was unable to start the python environment:", ex );
      }
    }
  }

  private void addConfigureTab() {
    wctiConfig = new CTabItem( wctfContainer, SWT.NONE );
    wctiConfig.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.ConfigTab.TabTitle" ) );

    wcConfig = new Composite( wctfContainer, SWT.NONE );
    props.setLook( wcConfig );
    FormLayout wflConfig = new FormLayout();
    wflConfig.marginWidth = 3;
    wflConfig.marginHeight = 3;
    wcConfig.setLayout( wflConfig );

    addRowHandlingGroup();
    addOptionsGroup();

    // Input Frames Label
    Label inputFramesLab = new Label( wcConfig, SWT.RIGHT );
    inputFramesLab.setText( BaseMessages.getString( PKG, "CPythonScriptExecutor.InputFrames.Label" ) );
    props.setLook( inputFramesLab );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( wgOptions, MARGIN );
    inputFramesLab.setLayoutData( fd );
    lastControl = inputFramesLab;

    // table
    ColumnInfo[]
        colinf =
        new ColumnInfo[] {
            new ColumnInfo( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.FrameNames.StepName" ),
                ColumnInfo.COLUMN_TYPE_CCOMBO, false ),
            new ColumnInfo( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.FrameNames.FrameName" ),
                ColumnInfo.COLUMN_TYPE_TEXT, false ) };

    String[] previousSteps = pipelineMeta.getPrevTransformNames( transformName );
    if ( previousSteps != null ) {
      colinf[0].setComboValues( previousSteps );
    }

    wtvInputFrames =
        new TableView( variables, wcConfig, SWT.FULL_SELECTION | SWT.MULTI, colinf, 1, simpleModifyListener, props );
    fd = new FormData();
    fd.top = new FormAttachment( lastControl, MARGIN );
    fd.bottom = new FormAttachment( 100, -MARGIN * 2 );
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    wtvInputFrames.setLayoutData( fd );

    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, -MARGIN * 2 );
    fd.bottom = new FormAttachment( 100, 0 );
    wcConfig.setLayoutData( fd );

    wcConfig.layout();
    wctiConfig.setControl( wcConfig );
  }

  private void addScriptTab() {
    wctiScript = new CTabItem( wctfContainer, SWT.NONE );
    wctiScript.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.ScriptTab.TabTitle" ) ); //$NON-NLS-1$
    wcScript = new Composite( wctfContainer, SWT.NONE );
    props.setLook( wcScript );
    FormLayout scriptLayout = new FormLayout();
    scriptLayout.marginWidth = 3;
    scriptLayout.marginHeight = 3;
    wcScript.setLayout( scriptLayout );

    wlLoadScriptFile = new Label( wcScript, SWT.RIGHT );
    props.setLook( wlLoadScriptFile );
    wlLoadScriptFile
        .setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.LoadScript.Label" ) ); //$NON-NLS-1$
    wlLoadScriptFile.setLayoutData( getFirstLabelFormData() );

    wbLoadScriptFile = new Button( wcScript, SWT.CHECK );
    props.setLook( wbLoadScriptFile );
    FormData fd = getFirstPromptFormData( wlLoadScriptFile );
    fd.right = null;
    wbLoadScriptFile.setLayoutData( fd );
    wbLoadScriptFile.setToolTipText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.LoadScript.TipText" ) );
    lastControl = wbLoadScriptFile;

    wbLoadScriptFile.addSelectionListener( new SelectionAdapter() {
      @Override public void widgetSelected( SelectionEvent e ) {
        checkWidgets();
      }
    } );

    wlScriptLocation = new Label( wcScript, SWT.RIGHT );
    props.setLook( wlScriptLocation );
    wlScriptLocation.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.ScriptFile.Label" ) );
    wlScriptLocation.setLayoutData( getFirstLabelFormData() );

    wbScriptBrowse = new Button( wcScript, SWT.PUSH | SWT.CENTER );
    wbScriptBrowse.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.Browse.Button" ) );
    props.setLook( wbScriptBrowse );
    fd = new FormData();
    fd.right = new FormAttachment( 100, -MARGIN );
    fd.top = new FormAttachment( lastControl, MARGIN );
    wbScriptBrowse.setLayoutData( fd );

    wbScriptBrowse.addSelectionListener( new SelectionAdapter() {
      @Override public void widgetSelected( SelectionEvent e ) {
        FileDialog dialog = new FileDialog( shell, SWT.OPEN );

        if ( !org.apache.hop.core.util.Utils.isEmpty( wtvScriptLocation.getText() ) ) {
          dialog.setFileName( variables.resolve( wtvScriptLocation.getText() ) );
        }

        if ( dialog.open() != null ) {
          wtvScriptLocation
              .setText( dialog.getFilterPath() + System.getProperty( "file.separator" ) + dialog.getFileName() );
        }
      }
    } );

    wtvScriptLocation = new TextVar( variables, wcScript, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wtvScriptLocation );
    fd = new FormData();
    fd.left = new FormAttachment( wlScriptLocation, MARGIN );
    fd.top = new FormAttachment( lastControl, MARGIN );
    fd.right = new FormAttachment( wbScriptBrowse, -MARGIN );
    wtvScriptLocation.setLayoutData( fd );
    lastControl = wtvScriptLocation;

    wlScript = new Label( wcScript, SWT.LEFT );
    props.setLook( wlScript );
    wlScript.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.ManualScript.Label" ) );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( lastControl, MARGIN );
    wlScript.setLayoutData( fd );
    lastControl = wlScript;

    // Use ScriptStyledTextComp for Python syntax highlighting in non-web environments
    if (EnvironmentUtils.getInstance().isWeb()) {
      wstcScriptEditor =
          new StyledTextComp( variables, wcScript, SWT.MULTI | SWT.LEFT | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL,
              true, false );
    } else {
      wstcScriptEditor =
          new ScriptStyledTextComp( variables, wcScript, SWT.MULTI | SWT.LEFT | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL,
              false );
      // Enable Python syntax highlighting
      ((ScriptStyledTextComp) wstcScriptEditor).addLineStyleListener("python");
    }
    props.setLook( wstcScriptEditor, Props.WIDGET_STYLE_FIXED );

    wlContinueOnUnsetVars = new Label( wcScript, SWT.RIGHT );
    props.setLook( wlContinueOnUnsetVars );
    wlContinueOnUnsetVars.setText( "Continue on unset variables" );
    wlContinueOnUnsetVars.setToolTipText(
        "Don't raise an error if specified variables are not set in python after " + "script has executed" );
    fd = new FormData();
    fd.bottom = new FormAttachment( 100, -MARGIN * 2 );
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( FIRST_LABEL_RIGHT_PERCENTAGE, 0 );
    wlContinueOnUnsetVars.setLayoutData( fd );

    wbContinueOnUnsetVars = new Button( wcScript, SWT.CHECK );
    props.setLook( wbContinueOnUnsetVars );
    fd = new FormData();
    fd.left = new FormAttachment( wlContinueOnUnsetVars, MARGIN );
    fd.right = new FormAttachment( SECOND_PROMPT_RIGHT_PERCENTAGE, 0 );
    fd.bottom = new FormAttachment( 100, -MARGIN * 2 );
    wbContinueOnUnsetVars.setLayoutData( fd );

    wlPyVarsToGet = new Label( wcScript, SWT.RIGHT );
    props.setLook( wlPyVarsToGet );
    wlPyVarsToGet.setText( "Python Variables to Get:" );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( FIRST_LABEL_RIGHT_PERCENTAGE, 0 );
    fd.bottom = new FormAttachment( wbContinueOnUnsetVars, -MARGIN );
    wlPyVarsToGet.setLayoutData( fd );

    wtvPyVarsToGet = new TextVar( variables, wcScript, SWT.SINGLE | SWT.LEAD | SWT.BORDER );
    props.setLook( wtvPyVarsToGet );
    fd = new FormData();
    fd.left = new FormAttachment( wlPyVarsToGet, MARGIN );
    fd.right = new FormAttachment( SECOND_PROMPT_RIGHT_PERCENTAGE, 0 );
    fd.bottom = new FormAttachment( wbContinueOnUnsetVars, -MARGIN );
    wtvPyVarsToGet.setLayoutData( fd );
    wtvPyVarsToGet.addFocusListener( new FocusAdapter() {
      @Override public void focusLost( FocusEvent e ) {
        super.focusLost( e );
        String currVars = wtvPyVarsToGet.getText();
        if ( !org.apache.hop.core.util.Utils.isEmpty( currVars ) ) {
          List<String> varList = stringToList( currVars );
          wbGetFields.setEnabled( varList.size() == 1 );
          wbIncludeRowIndex.setEnabled( varList.size() == 1 );
        }
      }
    } );

    /* Button wTestScriptScript = new Button( wcScript, SWT.PUSH );
    wTestScriptScript.setText( BaseMessages.getString( PKG, "RScriptExecutorDialog.TestScript.Button" ) );
    wTestScriptScript.setToolTipText(
        BaseMessages.getString( PKG, "RScriptExecutorDialog.TestScript.Button.TipText" ) );
    fd = new FormData();
    fd.bottom = new FormAttachment( 100, -MARGIN * 2 );
    fd.left = new FormAttachment( 0, 0 );
    wTestScriptScript.setLayoutData( fd );
    wTestScriptScript.addListener( SWT.Selection, new Listener() {
      @Override
      public void handleEvent( Event e ) {
        CPythonScriptExecutorMeta meta = new CPythonScriptExecutorMeta();
        getInfo( meta );
        testScript( meta );
      }
    } ); */

    wstcScriptEditor.addModifyListener( simpleModifyListener );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( lastControl, MARGIN );
    fd.right = new FormAttachment( 100, -2 * MARGIN );
    fd.bottom = new FormAttachment( wtvPyVarsToGet, -MARGIN );
    wstcScriptEditor.setLayoutData( fd );

    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.bottom = new FormAttachment( wtvPyVarsToGet, 0 );
    wcScript.setLayoutData( fd );

    wcScript.layout();
    wctiScript.setControl( wcScript );
  }

  private void addFieldsTab() {
    // --- fields tab
    wctiFields = new CTabItem( wctfContainer, SWT.NONE );
    wctiFields.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.FieldsTab.TabTitle" ) ); //$NON-NLS-1$
    wcFields = new Composite( wctfContainer, SWT.NONE );
    props.setLook( wcFields );
    FormLayout fieldsLayout = new FormLayout();
    fieldsLayout.marginWidth = 3;
    fieldsLayout.marginHeight = 3;
    wcFields.setLayout( fieldsLayout );

    wlOutputFields = new Label( wcFields, SWT.LEFT );
    wlOutputFields
        .setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.OutFields.Label" ) ); //$NON-NLS-1$
    props.setLook( wlOutputFields );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( MIDDLE, -MARGIN );
    fd.top = new FormAttachment( 0, MARGIN );
    wlOutputFields.setLayoutData( fd );
    lastControl = wlOutputFields;

    wbVarsToFields = new Button( wcFields, SWT.PUSH );
    wbVarsToFields
        .setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.VarsToFields.Button" ) ); //$NON-NLS-1$
    wbVarsToFields.setToolTipText(
        BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.VarsToFields.Button.TipText" ) ); //$NON-NLS-1$
    fd = new FormData();
    fd.bottom = new FormAttachment( 100, -MARGIN * 2 );
    fd.left = new FormAttachment( 0, 0 );
    wbVarsToFields.setLayoutData( fd );
    wbVarsToFields.addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( Event event ) {
        CPythonScriptExecutorMeta meta = new CPythonScriptExecutorMeta();
        setData( meta );
        varsToTableFields( meta );
      }
    } );
    wbGetFields = new Button( wcFields, SWT.PUSH );
    wbGetFields.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.GetFrameFields.Button" ) );
    wbGetFields
        .setToolTipText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.GetFrameFields.Button.TipText" ) );
    fd = new FormData();
    fd.bottom = new FormAttachment( 100, -MARGIN * 2 );
    fd.left = new FormAttachment( wbVarsToFields, MARGIN * 2 );
    wbGetFields.setLayoutData( fd );
    wbGetFields.addListener( SWT.Selection, new Listener() {
      @Override public void handleEvent( Event event ) {
        CPythonScriptExecutorMeta meta = new CPythonScriptExecutorMeta();
        setData( meta );
        getFrameFields( meta );
      }
    } );
    wbGetFields.setEnabled( false );

    Label wlIncludeRowIndex = new Label( wcFields, SWT.RIGHT );
    props.setLook( wlIncludeRowIndex );
    fd = new FormData();
    fd.bottom = new FormAttachment( 100, -MARGIN * 2 );
    fd.left = new FormAttachment( wbGetFields, MARGIN * 2 );
    wlIncludeRowIndex.setLayoutData( fd );

    wbIncludeRowIndex = new Button( wcFields, SWT.CHECK );
    wbIncludeRowIndex
        .setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.IncludeFrameRowIndex.Button" ) );
    fd = new FormData();
    fd.bottom = new FormAttachment( 100, -MARGIN * 2 );
    fd.left = new FormAttachment( wlIncludeRowIndex, MARGIN * 2 );
    wbIncludeRowIndex.setLayoutData( fd );
    wbIncludeRowIndex.setEnabled( false );

    // table
    ColumnInfo[]
        colinf2 =
        new ColumnInfo[] { new ColumnInfo( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.OutFields.Name" ),
            ColumnInfo.COLUMN_TYPE_TEXT, false ),
            new ColumnInfo( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.OutFields.Type" ),
                ColumnInfo.COLUMN_TYPE_CCOMBO, /*ValueMeta.getAllTypes()*/ ValueMetaFactory.getAllValueMetaNames(),
                false ) };

    wtvOutputFields =
        new TableView( variables, wcFields, SWT.FULL_SELECTION | SWT.MULTI, colinf2, 1, simpleModifyListener,
            props );
    fd = new FormData();
    fd.top = new FormAttachment( lastControl, MARGIN * 2 );
    fd.bottom = new FormAttachment( wbVarsToFields, -MARGIN * 2 );
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    wtvOutputFields.setLayoutData( fd );

    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.bottom = new FormAttachment( 100, 0 );
    wcFields.setLayoutData( fd );

    wcFields.layout();
    wctiFields.setControl( wcFields );
  }

  private void addRowHandlingGroup() {
    wgRowHandling = new Group( wcConfig, SWT.SHADOW_NONE );
    props.setLook( wgRowHandling );
    wgRowHandling.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.ConfigTab.RowHandlingGroup" ) );
    FormLayout wglRowHandling = new FormLayout();
    wglRowHandling.marginWidth = 10;
    wglRowHandling.marginHeight = 10;
    wgRowHandling.setLayout( wglRowHandling );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( 0, 0 );
    wgRowHandling.setLayoutData( fd );

    addRowsToProcessControllers(); // Number of Rows to Process
    addReservoirSamplingControllers(); // Reservoir Sampling
    addRandomSeedControllers(); // Random Seed
  }

  private void getFrameFields( CPythonScriptExecutorMeta meta ) {

    try {
      meta.setOutputFields( new RowMeta() );
      List<String> frameNames = meta.getFrameNames();
      List<IStream> infoStreams = meta.getStepIOMeta().getInfoStreams();
      List<IRowMeta> incomingMetas = new ArrayList<IRowMeta>();
      if ( frameNames.size() > 0 && infoStreams.size() > 0 ) {

        for ( int i = 0; i < infoStreams.size(); i++ ) {
          incomingMetas.add( pipelineMeta.getTransformFields( variables, infoStreams.get( i ).getTransformMeta() ) );
        }
      }

      ShowMessageDialog
          smd =
          new ShowMessageDialog( this.getParent(), SWT.YES | SWT.NO | SWT.ICON_WARNING,
              BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.GetFields.Dialog.Title" ),
              BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.GetFields.Dialog.Message" ), false );
      int buttonID = smd.open();

      if ( buttonID == SWT.YES ) {
        IRowMeta rowMeta = new RowMeta();
        meta.getFields( rowMeta, "bogus", incomingMetas.toArray( new IRowMeta[incomingMetas.size()] ), null,
            variables, null );

        wtvOutputFields.clearAll();
        for ( int i = 0; i < rowMeta.size(); i++ ) {
          TableItem item = new TableItem( wtvOutputFields.table, SWT.NONE );
          item.setText( 1, Const.NVL( rowMeta.getValueMeta( i ).getName(), "" ) );
          item.setText( 2, Const.NVL( rowMeta.getValueMeta( i ).getTypeDesc(), "" ) );
        }
        wtvOutputFields.removeEmptyRows();
        wtvOutputFields.setRowNums();
        wtvOutputFields.optWidth( true );
      }
    } catch ( HopException ex ) {
      new ErrorDialog( shell, transformName,
          BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.ErrorGettingFields" ), ex );
    }
  }

  private void addRowsToProcessControllers() {
    wlRowsToProcess = new Label( wgRowHandling, SWT.RIGHT );
    wlRowsToProcess.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.NumberOfRowsToProcess.Label" ) );
    props.setLook( wlRowsToProcess );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( FIRST_LABEL_RIGHT_PERCENTAGE, 0 );
    fd.top = new FormAttachment( lastControl, MARGIN );
    wlRowsToProcess.setLayoutData( getFirstLabelFormData() );

    wcvRowsToProcess = new ComboVar( variables, wgRowHandling, SWT.BORDER | SWT.READ_ONLY );
    props.setLook( wcvRowsToProcess );
    wcvRowsToProcess.setEditable( false );
    wcvRowsToProcess.addSelectionListener( new SelectionAdapter() {
      @Override public void widgetSelected( SelectionEvent e ) {
        m_inputMeta.setChanged();
        handleRowsToProcessChange();
        if ( wtvInputFrames.getItemCount() > 1 && wbReservoirSampling.getSelection() && wcvRowsToProcess.getText()
            .equals( BaseMessages.getString( PKG,
                "CPythonScriptExecutorDialog.NumberOfRowsToProcess.Dropdown.RowByRowEntry.Label" ) ) ) {
          ShowMessageDialog
              smd =
              new ShowMessageDialog( shell, SWT.OK | SWT.ICON_WARNING,
                  BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.RowByRowWarning.Title" ),
                  BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.RowByRowWarning.Message" ), false );
          smd.open();
        }
      }
    } );
    wcvRowsToProcess.setLayoutData( getFirstPromptFormData( wlRowsToProcess ) );
    wcvRowsToProcess.add(
        BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.NumberOfRowsToProcess.Dropdown.AllEntry.Label" ) );
    wcvRowsToProcess.add( BaseMessages
        .getString( PKG, "CPythonScriptExecutorDialog.NumberOfRowsToProcess.Dropdown.RowByRowEntry.Label" ) );
    wcvRowsToProcess.add(
        BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.NumberOfRowsToProcess.Dropdown.BatchEntry.Label" ) );
    wcvRowsToProcess.setToolTipText(
        BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.NumberOfRowsToProcess.Dropdown.TipText" ) );

    wlRowsToProcessSize = new Label( wgRowHandling, SWT.RIGHT );
    wlRowsToProcessSize
        .setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.NumberOfRowsToProcess.Size.Label" ) );
    props.setLook( wlRowsToProcessSize );
    wlRowsToProcessSize.setLayoutData( getSecondLabelFormData( wcvRowsToProcess ) );

    wtvRowsToProcessSize = new TextVar( variables, wgRowHandling, SWT.SINGLE | SWT.LEAD | SWT.BORDER );
    props.setLook( wtvRowsToProcessSize );
    wtvRowsToProcessSize.addModifyListener( simpleModifyListener );
    wtvRowsToProcessSize.setLayoutData( getSecondPromptFormData( wlRowsToProcessSize ) );
    wtvRowsToProcessSize.setEnabled( false );
    lastControl = wtvRowsToProcessSize;
    wtvRowsToProcessSize.setToolTipText(
        BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.NumberOfRowsToProcess.Size.TipText" ) );
  }

  private void addReservoirSamplingControllers() {
    //reservoir sampling
    wlReservoirSampling = new Label( wgRowHandling, SWT.RIGHT );
    wlReservoirSampling.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.ReservoirSampling.Label" ) );
    props.setLook( wlReservoirSampling );
    wlReservoirSampling.setLayoutData( getFirstLabelFormData() );

    wbReservoirSampling = new Button( wgRowHandling, SWT.CHECK );
    props.setLook( wbReservoirSampling );
    FormData fd = getFirstPromptFormData( wlReservoirSampling );
    fd.right = null;
    wbReservoirSampling.setLayoutData( fd );
    wbReservoirSampling.addSelectionListener( new SelectionAdapter() {
      @Override public void widgetSelected( SelectionEvent e ) {
        m_inputMeta.setChanged();
        handleReservoirSamplingChange();
        if ( wtvInputFrames.getItemCount() > 1 && wbReservoirSampling.getSelection() && wcvRowsToProcess.getText()
            .equals( BaseMessages.getString( PKG,
                "CPythonScriptExecutorDialog.NumberOfRowsToProcess.Dropdown.RowByRowEntry.Label" ) ) ) {
          ShowMessageDialog
              smd =
              new ShowMessageDialog( shell, SWT.OK | SWT.ICON_WARNING,
                  BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.RowByRowWarning.Title" ),
                  BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.RowByRowWarning.Message" ), false );
          smd.open();
        }
      }
    } );
    wbReservoirSampling
        .setToolTipText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.ReservoirSampling.TipText" ) );

    wlReservoirSamplingSize = new Label( wgRowHandling, SWT.RIGHT );
    wlReservoirSamplingSize
        .setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.ReservoirSampling.Size.Label" ) );
    props.setLook( wlReservoirSamplingSize );
    wlReservoirSamplingSize.setLayoutData( getSecondLabelFormData( wbReservoirSampling ) );

    wtvReservoirSamplingSize = new TextVar( variables, wgRowHandling, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wtvReservoirSamplingSize
        .setToolTipText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.ReservoirSampling.Size.TipText" ) );
    props.setLook( wtvReservoirSamplingSize );
    wtvReservoirSamplingSize.setLayoutData( getSecondPromptFormData( wlReservoirSamplingSize ) );
    wtvReservoirSamplingSize.setEnabled( false );
    lastControl = wtvReservoirSamplingSize;
    wtvReservoirSamplingSize.addModifyListener( new ModifyListener() {
      @Override public void modifyText( ModifyEvent e ) {
        m_inputMeta.setChanged();
      }
    } );
  }

  private void addRandomSeedControllers() {
    // random seed
    wlRandomSeed = new Label( wgRowHandling, SWT.RIGHT );
    wlRandomSeed.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.Seed.Label" ) );
    props.setLook( wlRandomSeed );
    wlRandomSeed.setLayoutData( getFirstLabelFormData() );

    wtvRandomSeed = new TextVar( variables, wgRowHandling, SWT.SINGLE | SWT.LEAD | SWT.BORDER );
    props.setLook( wtvRandomSeed );
    wtvRandomSeed.addModifyListener( simpleModifyListener );
    wtvRandomSeed.setLayoutData( getFirstPromptFormData( wlRandomSeed ) );
    lastControl = wtvRandomSeed;
    wtvRandomSeed.setToolTipText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.Seed.TipText" ) );
  }

  /**
   * Handles the change of the dropdown and performs the enable/disable of size, reservoir sampling and reservoir
   * sampling size
   */
  private void handleRowsToProcessChange() {
    //check and disable the size input if batch isn't selected
    String wcvRowsToProcessValue = wcvRowsToProcess.getText();
    if ( wcvRowsToProcessValue.equals( BaseMessages
        .getString( PKG, "CPythonScriptExecutorDialog.NumberOfRowsToProcess.Dropdown.BatchEntry.Label" ) ) ) {
      wtvRowsToProcessSize.setEnabled( true );
      setItemText( wtvRowsToProcessSize, m_originalMeta.getRowsToProcessSize() );

      //reset the other controllers
      wbReservoirSampling.setEnabled( false );
      wbReservoirSampling.setSelection( false );
      wtvReservoirSamplingSize.setEnabled( false );
      wtvReservoirSamplingSize.setText( "" );
      wtvRandomSeed.setEnabled( false );
      wtvRandomSeed.setText( "" );
    } else if ( wcvRowsToProcessValue.equals(
        BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.NumberOfRowsToProcess.Dropdown.AllEntry.Label" ) ) ) {
      wtvRowsToProcessSize.setEnabled( false );
      wtvRowsToProcessSize.setText( "" );

      //reset the other controllers
      wbReservoirSampling.setEnabled( true );
      wbReservoirSampling.setSelection( m_originalMeta.getDoingReservoirSampling() );
      wtvReservoirSamplingSize.setEnabled( wbReservoirSampling.getSelection() );
      setItemText( wtvReservoirSamplingSize, m_originalMeta.getReservoirSamplingSize() );
      wtvRandomSeed.setEnabled( m_originalMeta.getDoingReservoirSampling() );
      wtvRandomSeed.setText( m_originalMeta.getRandomSeed() == null ? "" : m_originalMeta.getRandomSeed() );
    } else if ( wcvRowsToProcessValue.equals( BaseMessages
        .getString( PKG, "CPythonScriptExecutorDialog.NumberOfRowsToProcess.Dropdown.RowByRowEntry.Label" ) ) ) {
      wtvRowsToProcessSize.setEnabled( false );
      wtvRowsToProcessSize.setText( "" );

      //reset the other controllers
      wbReservoirSampling.setEnabled( true );
      wbReservoirSampling.setSelection( m_originalMeta.getDoingReservoirSampling() );
      wtvReservoirSamplingSize.setEnabled( wbReservoirSampling.getSelection() );
      setItemText( wtvReservoirSamplingSize, m_originalMeta.getReservoirSamplingSize() );
      wtvRandomSeed.setEnabled( m_originalMeta.getDoingReservoirSampling() );
      setItemText( wtvRandomSeed, m_originalMeta.getRandomSeed() );
    }
  }

  private void addOptionsGroup() {
    // add second group
    wgOptions = new Group( wcConfig, SWT.SHADOW_NONE );
    props.setLook( wgOptions );
    wgOptions.setText( BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.ConfigTab.OptionsGroup" ) );
    FormLayout optionsGroupLayout = new FormLayout();
    optionsGroupLayout.marginWidth = 10;
    optionsGroupLayout.marginHeight = 10;
    wgOptions.setLayout( optionsGroupLayout );
    FormData fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( wgRowHandling, MARGIN );
    wgOptions.setLayoutData( fd );

    addIncludeInputInOutputControllers();
    addPythonOptions();
  }

  private void addIncludeInputInOutputControllers() {
    // Input Fields as Output Fields
    wlIncludeInputAsOutput = new Label( wgOptions, SWT.RIGHT );
    wlIncludeInputAsOutput.setText( BaseMessages.getString( PKG, "CPythonScriptExecutor.InputFieldAsOutput.Label" ) );

    props.setLook( wlIncludeInputAsOutput );
    wlIncludeInputAsOutput.setLayoutData( getFirstLabelFormData() );

    wbIncludeInputAsOutput = new Button( wgOptions, SWT.CHECK );
    props.setLook( wbIncludeInputAsOutput );
    FormData fd = getFirstPromptFormData( wlIncludeInputAsOutput );
    fd.right = null;
    wbIncludeInputAsOutput.setLayoutData( fd );
    lastControl = wbIncludeInputAsOutput;
    wbIncludeInputAsOutput.addSelectionListener( new SelectionAdapter() {
      @Override public void widgetSelected( SelectionEvent e ) {
        if ( wbIncludeInputAsOutput.getSelection() ) {
          ShowMessageDialog
              smd =
              new ShowMessageDialog( getParent(), SWT.OK | SWT.ICON_WARNING,
                  BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.InputFieldAsOutput.Dialog.Title" ),
                  BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.InputFieldAsOutput.Dialog.Message" ),
                  false );
          smd.open();
        }
        m_inputMeta.setChanged();
      }
    } );
    wbIncludeInputAsOutput
        .setToolTipText( BaseMessages.getString( PKG, "CPythonScriptExecutor.InputFieldAsOutput.TipText" ) );
  }

  private void addPythonOptions() {
    wlPythonCommand = new Label( wgOptions, SWT.RIGHT );
    wlPythonCommand.setText( BaseMessages.getString( PKG, "CPythonScriptExecutor.PythonCommand.Label" ) );
    wlPythonCommand.setToolTipText( BaseMessages.getString( PKG, "CPythonScriptExecutor.PythonCommand.TipText" ) );
    props.setLook( wlPythonCommand );
    wlPythonCommand.setLayoutData( getFirstLabelFormData() );

    wtvPythonCommand = new TextVar( variables, wgOptions, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wtvPythonCommand );
    FormData fd = getFirstPromptFormData( wlPythonCommand );
    fd.right = new FormAttachment( 95, 0 );
    wtvPythonCommand.setLayoutData( fd );
    lastControl = wtvPythonCommand;

    wlPyPathEntries = new Label( wgOptions, SWT.RIGHT );
    wlPyPathEntries.setText( BaseMessages.getString( PKG, "CPythonScriptExecutor.PyPathEntries.Label" ) );
    wlPyPathEntries.setToolTipText( BaseMessages.getString( PKG, "CPythonScriptExecutor.PyPathEntries.TipText" ) );
    props.setLook( wlPyPathEntries );
    wlPyPathEntries.setLayoutData( getFirstLabelFormData() );

    wtvPyPathEntries = new TextVar( variables, wgOptions, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wtvPyPathEntries );
    fd = getFirstPromptFormData( wlPyPathEntries );
    fd.right = new FormAttachment( 95, 0 );
    wtvPyPathEntries.setLayoutData( fd );
    lastControl = wtvPyPathEntries;

    wlPyServerID = new Label( wgOptions, SWT.RIGHT );
    wlPyServerID.setText( BaseMessages.getString( PKG, "CPythonScriptExecutor.PyServerID.Label" ) );
    wlPyServerID.setToolTipText( BaseMessages.getString( PKG, "CPythonScriptExecutor.PyServerID.TipText" ) );
    props.setLook( wlPyServerID );
    wlPyServerID.setLayoutData( getFirstLabelFormData() );

    wtvPyServerID = new TextVar( variables, wgOptions, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wtvPyServerID );
    fd = getFirstPromptFormData( wlPyServerID );
    fd.right = new FormAttachment( 95, 0 );
    wtvPyServerID.setLayoutData( fd );
    lastControl = wtvPyServerID;
  }

  protected void getData( CPythonScriptExecutorMeta meta ) {
    wcvRowsToProcess.setText( meta.getRowsToProcess() );
    setItemText( wtvRowsToProcessSize, meta.getRowsToProcessSize() );
    wbReservoirSampling.setSelection( meta.getDoingReservoirSampling() );
    setItemText( wtvReservoirSamplingSize, meta.getReservoirSamplingSize() );
    setItemText( wtvRandomSeed, Const.NVL( meta.getRandomSeed(), "" ) );
    wbIncludeInputAsOutput.setSelection( meta.getIncludeInputAsOutput() );
    setItemText( wtvPyVarsToGet, listToString( meta.getPythonVariablesToGet() ) );
    wbContinueOnUnsetVars.setSelection( meta.getContinueOnUnsetVars() );
    setItemText( wtvPythonCommand, meta.getPythonCommand() );
    setItemText( wtvPyPathEntries, meta.getPyPathEntries() );
    setItemText( wtvPyServerID, meta.getPytServerID() );
    wstcScriptEditor.setText( meta.getScript() == null ? "" : meta.getScript() ); //$NON-NLS-1$
    wbLoadScriptFile.setSelection( meta.getLoadScriptAtRuntime() );
    setItemText( wtvScriptLocation, meta.getScriptToLoad() );
    wbIncludeRowIndex.setSelection( meta.getIncludeFrameRowIndexAsOutputField() );

    setInputToFramesTableFields( meta );
    setOutputFieldsTableFields( meta );

    checkWidgets();
    handleRowsToProcessChange();
    handleReservoirSamplingChange();
  }

  private void varsToTableFields( CPythonScriptExecutorMeta meta ) {
    // List<IRowMeta> incomingMetas;
    IRowMeta incomingMetas = new RowMeta();
    if ( meta.getIncludeInputAsOutput() ) {
      List<String> frameNames = meta.getFrameNames();
      List<IStream> infoStreams = meta.getStepIOMeta().getInfoStreams();
      if ( frameNames.size() > 0 && infoStreams.size() > 0 ) {
        // incomingMetas = new ArrayList<IRowMeta>();

        try {
          for ( int i = 0; i < infoStreams.size(); i++ ) {
            incomingMetas.addRowMeta( pipelineMeta.getTransformFields( variables, infoStreams.get( i ).getTransformMeta() ) );
          }
        } catch ( HopException e ) {
          new ErrorDialog( shell, transformName,
              BaseMessages.getString( PKG, "CPythonScriptExecutorDialog.ErrorGettingFields" ), e );
          return;
        }
      }
    }

    wtvOutputFields.clearAll();
    if ( incomingMetas.size() > 0 ) {
      for ( IValueMeta vm : incomingMetas.getValueMetaList() ) {
        TableItem item = new TableItem( wtvOutputFields.table, SWT.NONE );
        item.setText( 1, Const.NVL( vm.getName(), "" ) );
        item.setText( 2, Const.NVL( vm.getTypeDesc(), "" ) );
      }
    }
    String vars = wtvPyVarsToGet.getText();
    if ( !org.apache.hop.core.util.Utils.isEmpty( vars ) ) {
      String[] vA = vars.split( "," );
      if ( vA.length > 0 ) {
        for ( String var : vA ) {
          TableItem item = new TableItem( wtvOutputFields.table, SWT.NONE );
          item.setText( 1, Const.NVL( var.trim(), "" ) );
          item.setText( 2, "String" );
        }
        wtvOutputFields.removeEmptyRows();
        wtvOutputFields.setRowNums();
        wtvOutputFields.optWidth( true );
      }
    }
  }

  private List<String> stringToList( String list ) {
    List<String> result = new ArrayList<String>();
    for ( String s : list.split( "," ) ) {
      if ( !org.apache.hop.core.util.Utils.isEmpty( s.trim() ) ) {
        result.add( s.trim() );
      }
    }

    return result;
  }

  private String listToString( List<String> list ) {
    StringBuilder b = new StringBuilder();
    for ( String s : list ) {
      b.append( s ).append( "," );
    }
    if ( b.length() > 0 ) {
      b.setLength( b.length() - 1 );
    }

    return b.toString();
  }

  private void setData( CPythonScriptExecutorMeta meta ) {
    meta.setRowsToProcess( wcvRowsToProcess.getText() );
    meta.setRowsToProcessSize( wtvRowsToProcessSize.getText() );
    meta.setDoingReservoirSampling( wbReservoirSampling.getSelection() );
    meta.setReservoirSamplingSize( wtvReservoirSamplingSize.getText() );
    meta.setRandomSeed( wtvRandomSeed.getText() );
    meta.setContinueOnUnsetVars( wbContinueOnUnsetVars.getSelection() );
    meta.setPythonVariablesToGet( stringToList( wtvPyVarsToGet.getText() ) );
    meta.setPythonCommand( wtvPythonCommand.getText() );
    meta.setPyPathEntries( wtvPyPathEntries.getText() );
    meta.setPyServerID( wtvPyServerID.getText() );
    meta.setIncludeInputAsOutput( wbIncludeInputAsOutput.getSelection() );
    meta.setScript( wstcScriptEditor.getText() );
    meta.setLoadScriptAtRuntime( wbLoadScriptFile.getSelection() );
    meta.setScriptToLoad( wtvScriptLocation.getText() );
    meta.setIncludeFrameRowIndexAsOutputField( wbIncludeRowIndex.getSelection() );

    // incoming stream/frame name data from table
    int numNonEmpty = wtvInputFrames.nrNonEmpty();
    List<String> frameNames = new ArrayList<String>();
    List<String> stepNames = new ArrayList<String>();
    meta.clearStepIOMeta();
    for ( int i = 0; i < numNonEmpty; i++ ) {
      TableItem item = wtvInputFrames.getNonEmpty( i );
      String stepName = item.getText( 1 ).trim();
      String frameName = item.getText( 2 ).trim();
      if ( !org.apache.hop.core.util.Utils.isEmpty( stepName ) ) {
        if ( org.apache.hop.core.util.Utils.isEmpty( frameName ) ) {
          frameName = CPythonScriptExecutorMeta.DEFAULT_FRAME_NAME_PREFIX + i;
        }
        frameNames.add( frameName );
        stepNames.add( stepName );
      }
    }

    meta.setFrameNames( frameNames );
    List<IStream> infoStreams = meta.getStepIOMeta().getInfoStreams();
    for ( int i = 0; i < infoStreams.size(); i++ ) {
      infoStreams.get( i ).setSubject( stepNames.get( i ) );
    }

    // output field data from table
    numNonEmpty = wtvOutputFields.nrNonEmpty();
    IRowMeta outRM = numNonEmpty > 0 ? new RowMeta() : null;
    for ( int i = 0; i < numNonEmpty; i++ ) {
      TableItem item = wtvOutputFields.getNonEmpty( i );
      String name = item.getText( 1 ).trim();
      String type = item.getText( 2 ).trim();
      if ( !org.apache.hop.core.util.Utils.isEmpty( name ) && !org.apache.hop.core.util.Utils.isEmpty( type ) ) {
        //IValueMeta vm = new ValueMeta( name, ValueMeta.getType( type ) );
        IValueMeta vm;
        try {
          vm = ValueMetaFactory.createValueMeta( name, ValueMetaFactory.getIdForValueMeta( type ) );
          outRM.addValueMeta( vm );
        } catch ( HopPluginException e ) {
          e.printStackTrace();
        }
      }
    }

    meta.setOutputFields( outRM );
  }

  private void handleReservoirSamplingChange() {
    wtvReservoirSamplingSize.setEnabled( wbReservoirSampling.getSelection() );
    wtvRandomSeed.setEnabled( wbReservoirSampling.getSelection() );
  }

  protected void checkWidgets() {
    wtvScriptLocation.setEnabled( wbLoadScriptFile.getSelection() );
    wstcScriptEditor.setEnabled( !wbLoadScriptFile.getSelection() );
    if ( wbLoadScriptFile.getSelection() ) {
      wtvScriptLocation.setEditable( true );
      // Handle both StyledTextComp and ScriptStyledTextComp cases
      if ( wstcScriptEditor instanceof StyledTextComp ) {
        ((StyledTextComp) wstcScriptEditor).getTextWidget().setBackground( GuiResource.getInstance().getColorDemoGray() );
      }
    } else {
      wtvScriptLocation.setEditable( false );
      props.setLook(wstcScriptEditor, Props.WIDGET_STYLE_FIXED);
    }
    wbScriptBrowse.setEnabled( wbLoadScriptFile.getSelection() );

    String currVars = wtvPyVarsToGet.getText();
    if ( !org.apache.hop.core.util.Utils.isEmpty( currVars ) ) {
      List<String> varList = stringToList( currVars );
      wbGetFields.setEnabled( varList.size() == 1 );
      wbIncludeRowIndex.setEnabled( varList.size() == 1 );
    }
  }

  protected void setInputToFramesTableFields( CPythonScriptExecutorMeta meta ) {
    List<String> frameNames = meta.getFrameNames();
    // List<IStream> infoStreams = meta.getStepIOMeta().getInfoStreams();
    List<IStream> infoStreams = meta.getStepIOMeta().getInfoStreams();

    wtvInputFrames.clearAll();
    for ( int i = 0; i < infoStreams.size(); i++ ) {
      if ( infoStreams.get( i ).getSubject() != null ) {
        String stepName = infoStreams.get( i ).getSubject().toString();
        String frameName = frameNames.get( i );

        TableItem item = new TableItem( wtvInputFrames.table, SWT.NONE );
        item.setText( 1, Const.NVL( stepName, "" ) ); //$NON-NLS-1$
        item.setText( 2, Const.NVL( frameName, "" ) ); //$NON-NLS-1$

        // TransformMeta m = pipelineMeta.findTransform(stepName);
        // infoStreams.get(i).setTransformMeta(m);
      }
    }

    wtvInputFrames.removeEmptyRows();
    wtvInputFrames.setRowNums();
    wtvInputFrames.optWidth( true );
  }

  protected void setOutputFieldsTableFields( CPythonScriptExecutorMeta meta ) {
    IRowMeta outFields = meta.getOutputFields();

    if ( outFields != null && outFields.size() > 0 ) {
      for ( int i = 0; i < outFields.size(); i++ ) {
        IValueMeta vm = outFields.getValueMeta( i );
        String name = vm.getName();
        String type = vm.getTypeDesc();

        TableItem item = new TableItem( wtvOutputFields.table, SWT.NONE );
        item.setText( 1, Const.NVL( name, "" ) ); //$NON-NLS-1$
        item.setText( 2, Const.NVL( type, "" ) ); //$NON-NLS-1$
      }

      wtvOutputFields.removeEmptyRows();
      wtvOutputFields.setRowNums();
      wtvOutputFields.optWidth( true );
    }
  }

  private void cancel() {
    transformName = null;
    m_inputMeta.setChanged( changed );
    dispose();
  }

  /**
   * Ok general method
   */
  private void ok() {
    if ( org.apache.hop.core.util.Utils.isEmpty( wTransformName.getText() ) ) {
      return;
    }

    transformName = wTransformName.getText(); // return value

    setData( m_inputMeta );
    if ( !m_originalMeta.equals( m_inputMeta ) ) {
      m_inputMeta.setChanged();
      changed = m_inputMeta.hasChanged();
    }

    dispose();
  }

  private FormData getFirstLabelFormData() {
    FormData fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( FIRST_LABEL_RIGHT_PERCENTAGE, 0 );
    fd.top = new FormAttachment( lastControl, MARGIN );
    return fd;
  }

  private FormData getSecondLabelFormData( Control prevControl ) {
    FormData fd = new FormData();
    fd.left = new FormAttachment( prevControl, 0 );
    fd.right = new FormAttachment( SECOND_LABEL_RIGHT_PERCENTAGE, 0 );
    fd.top = new FormAttachment( lastControl, MARGIN );
    return fd;
  }

  private FormData getFirstPromptFormData( Control prevControl ) {
    FormData fd = new FormData();
    fd.left = new FormAttachment( prevControl, MARGIN );
    fd.right = new FormAttachment( FIRST_PROMPT_RIGHT_PERCENTAGE, 0 );
    fd.top = new FormAttachment( lastControl, MARGIN );
    return fd;
  }

  private FormData getSecondPromptFormData( Control prevControl ) {
    FormData fd = new FormData();
    fd.left = new FormAttachment( prevControl, MARGIN );
    fd.top = new FormAttachment( lastControl, MARGIN );
    fd.right = new FormAttachment( SECOND_PROMPT_RIGHT_PERCENTAGE, 0 );
    return fd;
  }

  /**
   * SWT TextVar's throw IllegalArgumentExceptions for null.  Defend against this.
   *
   * @param item  - A TextVar, but could be generalized as needed.
   * @param value - Value to set
   */
  private void setItemText( TextVar item, String value ) {
    item.setText( value == null ? "" : value );
  }
}
