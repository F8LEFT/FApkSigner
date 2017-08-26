package f8left;

import com.android.apksigner.ApkSignerTool;
import com.google.gson.Gson;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;

public class Controller {
    @FXML
    private TextField apk_src_path;
    @FXML
    private TextField apk_out_path;
    @FXML
    private TextField key_store_path;
    @FXML
    private TextField key_store_pass;
    @FXML
    private ComboBox key_alias;
    @FXML
    private TextField key_pass;
    @FXML
    private CheckBox pass_save;
    @FXML
    private CheckBox v1_sign;
    @FXML
    private CheckBox v2_sign;
    @FXML
    private Label status;

    private File configFile;
    private SignConfig signConfig;

    @FXML
    public void initialize() {
        apk_src_path.setOnDragOver(new DragOverEvent(apk_src_path));
        apk_src_path.setOnDragDropped(new DragDroppedEvent(apk_src_path));
        apk_out_path.setOnDragOver(new DragOverEvent(apk_out_path));
        apk_out_path.setOnDragDropped(new DragDroppedEvent(apk_out_path));
        key_store_path.setOnDragOver(new DragOverEvent(key_store_path));
        key_store_path.setOnDragDropped(new DragDroppedEvent(key_store_path));

        apk_src_path.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                onApkSrcChange();
            }
        });
        key_store_pass.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                onKsPassChange();
            }
        });


        File jarDir = new File(Controller.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        while(!jarDir.isDirectory()) jarDir = jarDir.getParentFile();
        configFile = new File(jarDir, "signconfig.json");

        loadConfig();
    }

    private FileFilter apkFilter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            if(f.getName().endsWith(".apk") || f.isDirectory()) {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Apk File(*.apk)";
        }
    };

    @FXML
    protected void openApkSrc() {
        JFileChooser jfc=new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setFileFilter(apkFilter);
        jfc.showOpenDialog(new JLabel());
        File file=jfc.getSelectedFile();
        if(file.exists() && file.isFile()) {
            apk_src_path.setText(file.getAbsolutePath());

        } else {
            status.setText("Input apk file not valid");
        }
    }

    @FXML
    protected void openApkOut() {
        String srcApkPath = apk_src_path.getText();
        File srcApk = new File(srcApkPath);
        if(!srcApk.exists() || !srcApk.isFile()) {
            status.setText("Not valid apk input, please choose source apk file");
            return;
        }
        JFileChooser jfc=new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jfc.setFileFilter(apkFilter);
        jfc.showOpenDialog(new JLabel());
        File file=jfc.getSelectedFile();
        if(file.isDirectory()) {
            String apkName = srcApk.getName();
            if(apkName.endsWith(".apk")) apkName = apkName.replace(".apk", "-sign.apk");
            apk_out_path.setText(new File(file, apkName).getAbsolutePath());
        } else {
            apk_out_path.setText(file.getAbsolutePath());
        }
    }

    @FXML
    protected void openKsPath() {
        JFileChooser jfc=new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String name = f.getName();
                if(name.endsWith(".jks") || name.endsWith(".keystore")) {
                    return true;
                }
                if(f.isDirectory()) return true;
                return false;
            }

            @Override
            public String getDescription() {
                return "android keystore(*.jks, *.keystore)";
            }
        });
        jfc.showOpenDialog(new JLabel());
        File file=jfc.getSelectedFile();
        // TODO try to parse ks
        if(file.exists() && file.isFile()) {
            key_store_path.setText(file.getAbsolutePath());
        } else {
            status.setText("Input key store file is not valid");
        }
    }

    @FXML
    protected void onKsPassChange() {
        String pass = key_store_pass.getText();
        File kfile = new File(key_store_path.getText());
        if(!kfile.isFile()) return;
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            if(parseKsFile(ks, kfile, pass)) {
            // read keystore file
                Enumeration<String> aliases = ks.aliases();
                ArrayList<String> rel = new ArrayList<>();
                while(aliases.hasMoreElements()) {
                    String alia = aliases.nextElement();
                    rel.add(alia);
                }
                if(rel.isEmpty()) return;
                ObservableList<String> olist = FXCollections.observableArrayList(rel);
                key_alias.setItems(olist);
                key_alias.setValue(rel.get(0));
            } else {
                key_alias.setItems(null);
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onApkSrcChange() {
        File file = new File(apk_src_path.getText());
        String apkName = file.getName();
        if(apkName.endsWith(".apk")) apkName = apkName.replace(".apk", "-sign.apk");
        else apkName = apkName + "-sign.apk";
        apk_out_path.setText(new File(file.getParentFile(), apkName).getAbsolutePath());
    }

    @FXML
    protected void doSign() {
        saveConfig(pass_save.isSelected());

        ArrayList<String> param = new ArrayList<String>();
        param.add("sign");
        param.add("--ks");
        param.add(key_store_path.getText());
        param.add("--ks-key-alias");
        param.add((String) key_alias.getValue());
        param.add("--ks-pass");
        param.add("pass:" + key_store_pass.getText());
        param.add("--key-pass");
        param.add("pass:" + key_pass.getText());
        if(v1_sign.isSelected()) {
            param.add("--v1-signing-enabled");
            param.add("true");
        }
        if(v2_sign.isSelected()) {
            param.add("--v2-signing-enabled");
            param.add("true");
        }
        param.add("--out");
        param.add(apk_out_path.getText());
        param.add("--in");
        param.add(apk_src_path.getText());
        try {
            ApkSignerTool.main( param.toArray(new String[param.size()]));
            status.setText("Sign success");
        } catch (Exception e){
            status.setText("Sign error: " + e.getMessage());
        }
    }

    @FXML
    protected void doCancel() {
        saveConfig(pass_save.isSelected());
        System.exit(0);
    }

    @FXML
    protected void doSave() {
        saveConfig(pass_save.isSelected());
    }

    private boolean parseKsFile(KeyStore ks, File file, String password) {
        // try to load keystore
        try {
            if (file != null) {
                try (FileInputStream in = new FileInputStream(file)) {
                    ks.load(in, password.toCharArray());
                }
            } else {
                ks.load(null, password.toCharArray());
            }
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    private boolean saveConfig(boolean savePassword) {
        Gson gson = new Gson();
        signConfig.apk_src = apk_src_path.getText();
        signConfig.apk_out = apk_out_path.getText();

        signConfig.ks_path = key_store_path.getText();
        signConfig.remember_pass = savePassword;
        signConfig.v1_sign = v1_sign.isSelected();
        signConfig.v2_sign = v2_sign.isSelected();
        if(savePassword) {
            signConfig.ks_alias = (String) key_alias.getValue();
            signConfig.ks_pass = key_store_pass.getText();
            signConfig.key_pass = key_pass.getText();
        } else {
            signConfig.ks_alias = "";
            signConfig.key_pass = "";
            signConfig.key_pass = "";
        }

        String json = gson.toJson(signConfig);
        return writeFile(configFile, json);
    }

    private boolean loadConfig() {
        Gson gson = new Gson();
        try {
            String jdata = readFile(configFile);
            signConfig = gson.fromJson(jdata, SignConfig.class);
            apk_src_path.setText(signConfig.apk_src);
            apk_out_path.setText(signConfig.apk_out);

            key_store_path.setText(signConfig.ks_path);
            v1_sign.setSelected(signConfig.v1_sign);
            v2_sign.setSelected(signConfig.v2_sign);
            pass_save.setSelected(signConfig.remember_pass);
            if(signConfig.remember_pass) {
                key_store_pass.setText(signConfig.ks_pass);
                key_alias.setValue(signConfig.ks_alias);
                key_pass.setText(signConfig.key_pass);
            }
        } catch (Exception e) {
            signConfig = new SignConfig();
        }
        return true;
    }

    public static String readFile(File file) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder builder = new StringBuilder();
            String s = null;
            while((s = br.readLine()) != null) {
                builder.append(s);
            }
            br.close();
            return builder.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean writeFile(File file, String data) {
        try {
            FileOutputStream os = new FileOutputStream(file);
            os.write(data.getBytes());
            os.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public class DragOverEvent implements EventHandler<DragEvent> {
        private TextField textField;
        public DragOverEvent(TextField textField){
            this.textField = textField;
        }
        public void handle(DragEvent event) {
            if (event.getGestureSource() != textField){
                event.acceptTransferModes(TransferMode.ANY);
            }
        }
    }

    public class DragDroppedEvent implements EventHandler<DragEvent> {
        private TextField textField;
        public DragDroppedEvent(TextField textField){
            this.textField = textField;
        }
        public void handle(DragEvent event) {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles()){
                try {
                    File file = dragboard.getFiles().get(0);
                    if (file != null) {
                        textField.setText(file.getAbsolutePath());
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}