package com.personal.gui;

import com.personal.CryptoCategory;
import com.personal.CryptoContext;
import com.personal.CryptoMethod;
import com.personal.register.CryptoRegistry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class App extends JFrame {

    // ===== 顶部算法选择 =====
    private final JComboBox<CryptoCategory> categoryBox = new JComboBox<>();
    private final JComboBox<CryptoMethod>   methodBox   = new JComboBox<>();
    private final JLabel descriptionLabel = new JLabel(" ");

    // ===== 动态输入区 =====
    private final JPanel inputPanel = new JPanel();
    private final JPasswordField passwordField    = new JPasswordField(20);
    private final JTextField     keyFileField     = new JTextField(24);
    private final JTextField     publicKeyField   = new JTextField(24);
    private final JTextField     privateKeyField  = new JTextField(24);
    private final Map<String, JTextField> extraFields = new LinkedHashMap<>();

    // ===== 文件列表 =====
    private final DefaultListModel<File> fileModel = new DefaultListModel<>();
    private final JList<File> fileList = new JList<>(fileModel);
    private final JCheckBox deleteOriginal = new JCheckBox("完成后删除原文件");
    private File lastDir;   // 记住上次选择的文件夹/文件目录

    // ===== 操作按钮 =====
    private final JButton encryptBtn = new JButton("加密");
    private final JButton decryptBtn = new JButton("解密");
    private final JButton hashBtn    = new JButton("哈希");
    private final JButton signBtn    = new JButton("签名");
    private final JButton verifyBtn  = new JButton("验证");

    // ===== 状态区 =====
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel statusLabel = new JLabel("就绪");

    public App() {
        super("文件加密工具");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        buildUI();
        loadCategories();
        refreshMethodList();
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // ============ 顶部 ============
        JPanel top = new JPanel(new BorderLayout(6, 6));

        JPanel selectRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        selectRow.add(new JLabel("分类："));
        categoryBox.setPreferredSize(new Dimension(150, categoryBox.getPreferredSize().height));
        selectRow.add(categoryBox);
        selectRow.add(new JLabel("算法："));
        methodBox.setPreferredSize(new Dimension(220, methodBox.getPreferredSize().height));
        selectRow.add(methodBox);
        top.add(selectRow, BorderLayout.NORTH);

        descriptionLabel.setForeground(Color.GRAY);
        descriptionLabel.setBorder(new EmptyBorder(0, 4, 4, 0));
        top.add(descriptionLabel, BorderLayout.CENTER);

        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBorder(BorderFactory.createTitledBorder("参数"));
        top.add(inputPanel, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // ============ 中央 ============
        JPanel center = new JPanel(new BorderLayout(6, 6));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(0, 0, 0, 4));
        JButton addFiles  = new JButton("添加文件");
        JButton addFolder = new JButton("添加文件夹");
        JButton removeSel = new JButton("移除选中");
        JButton clearAll  = new JButton("清空列表");
        addFiles.addActionListener(e -> chooseFiles());
        addFolder.addActionListener(e -> chooseFolder());
        removeSel.addActionListener(e -> removeSelected());
        clearAll.addActionListener(e -> fileModel.clear());
        for (JButton b : new JButton[]{addFiles, addFolder, removeSel, clearAll}) {
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            left.add(b);
            left.add(Box.createVerticalStrut(6));
        }
        left.setPreferredSize(new Dimension(150, 0));
        center.add(left, BorderLayout.WEST);

        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                File f = (File) value;
                setText(f.getAbsolutePath());
                setToolTipText(f.getAbsolutePath());
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(fileList);
        scroll.setBorder(BorderFactory.createTitledBorder("待处理文件（Ctrl / Shift 多选）"));
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // ============ 底部 ============
        JPanel bottom = new JPanel(new BorderLayout(6, 6));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.add(deleteOriginal);
        btnRow.add(Box.createHorizontalStrut(12));
        btnRow.add(encryptBtn);
        btnRow.add(decryptBtn);
        btnRow.add(hashBtn);
        btnRow.add(signBtn);
        btnRow.add(verifyBtn);
        encryptBtn.addActionListener(e -> runOperation(Operation.ENCRYPT));
        decryptBtn.addActionListener(e -> runOperation(Operation.DECRYPT));
        hashBtn.addActionListener(e    -> runOperation(Operation.HASH));
        signBtn.addActionListener(e    -> runOperation(Operation.SIGN));
        verifyBtn.addActionListener(e  -> runOperation(Operation.VERIFY));
        bottom.add(btnRow, BorderLayout.NORTH);

        progressBar.setStringPainted(true);
        JPanel status = new JPanel(new BorderLayout(0, 4));
        status.add(progressBar, BorderLayout.NORTH);
        status.add(statusLabel, BorderLayout.SOUTH);
        bottom.add(status, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        // ============ 事件 ============
        categoryBox.addActionListener(e -> refreshMethodList());
        methodBox.addActionListener(e -> {
            updateDescription();
            rebuildInputPanel();
            updateButtons();
        });
    }

    // ============ 顶部选择逻辑 ============

    private void loadCategories() {
        categoryBox.removeAllItems();
        for (CryptoCategory c : CryptoCategory.values()) categoryBox.addItem(c);
    }

    private void refreshMethodList() {
        CryptoCategory cat = (CryptoCategory) categoryBox.getSelectedItem();
        methodBox.removeAllItems();
        if (cat == null) return;
        for (CryptoMethod m : CryptoRegistry.byCategory(cat)) methodBox.addItem(m);

        methodBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                setText(value instanceof CryptoMethod ? ((CryptoMethod) value).getName() : "");
                return this;
            }
        });

        updateDescription();
        rebuildInputPanel();
        updateButtons();
    }

    private void updateDescription() {
        CryptoMethod m = (CryptoMethod) methodBox.getSelectedItem();
        descriptionLabel.setText(m == null ? " " : m.getDescription());
    }

    /** 根据算法声明重建参数输入区 */
    private void rebuildInputPanel() {
        inputPanel.removeAll();
        extraFields.clear();

        CryptoMethod m = (CryptoMethod) methodBox.getSelectedItem();
        if (m == null) {
            inputPanel.revalidate();
            inputPanel.repaint();
            return;
        }

        if (m.needsPassword())   inputPanel.add(row("密码：",     passwordField, makeShowPasswordBox()));
        if (m.needsKeyFile())    inputPanel.add(fileRow("对称密钥：", keyFileField));
        if (m.needsPublicKey())  inputPanel.add(fileRow("公钥文件：", publicKeyField));
        if (m.needsPrivateKey()) inputPanel.add(fileRow("私钥文件：", privateKeyField));

        String[] names    = m.getExtraParamNames();
        String[] defaults = m.getExtraParamDefaults();
        for (int i = 0; i < names.length; i++) {
            JTextField tf = new JTextField(20);
            if (i < defaults.length) tf.setText(defaults[i]);
            extraFields.put(names[i], tf);
            inputPanel.add(row(names[i] + "：", tf, null));
        }

        if (inputPanel.getComponentCount() == 0) {
            JLabel empty = new JLabel("该算法不需要额外参数");
            empty.setForeground(Color.GRAY);
            empty.setBorder(new EmptyBorder(4, 6, 4, 0));
            inputPanel.add(empty);
        }

        inputPanel.revalidate();
        inputPanel.repaint();
    }

    private JPanel row(String label, JComponent field, JComponent extra) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        p.add(new JLabel(label));
        p.add(field);
        if (extra != null) p.add(extra);
        return p;
    }

    private JPanel fileRow(String label, JTextField field) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        p.add(new JLabel(label));
        field.setEditable(false);
        p.add(field);
        JButton choose = new JButton("选择...");
        choose.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                field.setText(fc.getSelectedFile().getAbsolutePath());
        });
        p.add(choose);
        JButton clear = new JButton("清除");
        clear.addActionListener(e -> field.setText(""));
        p.add(clear);
        return p;
    }

    private JComponent makeShowPasswordBox() {
        JCheckBox show = new JCheckBox("显示");
        show.addActionListener(e ->
                passwordField.setEchoChar(show.isSelected() ? (char) 0 : '•'));
        return show;
    }

    private void updateButtons() {
        CryptoMethod m = (CryptoMethod) methodBox.getSelectedItem();
        boolean has = m != null;
        encryptBtn.setEnabled(has && m.canEncrypt());
        decryptBtn.setEnabled(has && m.canDecrypt());
        hashBtn.setEnabled   (has && m.canHash());
        signBtn.setEnabled   (has && m.canSign());
        verifyBtn.setEnabled (has && m.canVerify());
    }

    // ============ 文件选择 ============

    private void chooseFiles() {
        JFileChooser fc = new JFileChooser();
        rememberLastDir(fc);
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            lastDir = fc.getCurrentDirectory();
            for (File f : fc.getSelectedFiles()) addFile(f);
        }
    }

    private void chooseFolder() {
        JFileChooser fc = new JFileChooser();
        rememberLastDir(fc);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            lastDir = fc.getSelectedFile();
            File[] files = fc.getSelectedFile().listFiles(File::isFile);
            if (files != null) for (File f : files) addFile(f);
        }
    }

    /** 让文件选择框从上次选择的位置打开 */
    private void rememberLastDir(JFileChooser fc) {
        if (lastDir != null && lastDir.isDirectory())
            fc.setCurrentDirectory(lastDir);
    }

    private void addFile(File f) {
        for (int i = 0; i < fileModel.size(); i++)
            if (fileModel.get(i).equals(f)) return;
        fileModel.addElement(f);
    }

    private void removeSelected() {
        int[] idx = fileList.getSelectedIndices();
        for (int i = idx.length - 1; i >= 0; i--) fileModel.remove(idx[i]);
    }

    // ============ 执行 ============

    private enum Operation { ENCRYPT, DECRYPT, HASH, SIGN, VERIFY }

    private void runOperation(Operation op) {
        CryptoMethod m = (CryptoMethod) methodBox.getSelectedItem();
        if (m == null) {
            JOptionPane.showMessageDialog(this, "请先选择算法");
            return;
        }
        if (fileModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先添加要处理的文件");
            return;
        }

        // 收集参数
        CryptoContext ctx = new CryptoContext();
        ctx.password = passwordField.getPassword();
        if (!keyFileField.getText().isEmpty())    ctx.keyFile       = new File(keyFileField.getText());
        if (!publicKeyField.getText().isEmpty())  ctx.publicKeyFile = new File(publicKeyField.getText());
        if (!privateKeyField.getText().isEmpty()) ctx.privateKeyFile= new File(privateKeyField.getText());
        for (Map.Entry<String, JTextField> e : extraFields.entrySet())
            ctx.extra.put(e.getKey(), e.getValue().getText());

        // 简单校验
        if (m.needsPassword()   && (ctx.password == null || ctx.password.length == 0))
        { JOptionPane.showMessageDialog(this, "该算法需要密码"); return; }
        if (m.needsKeyFile()    && ctx.keyFile        == null)
        { JOptionPane.showMessageDialog(this, "请选择对称密钥文件"); return; }
        if (m.needsPublicKey()  && ctx.publicKeyFile  == null)
        { JOptionPane.showMessageDialog(this, "请选择公钥文件"); return; }
        if (m.needsPrivateKey() && ctx.privateKeyFile == null)
        { JOptionPane.showMessageDialog(this, "请选择私钥文件"); return; }

        List<File> selected = fileList.getSelectedValuesList();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选中要处理的文件");
            return;
        }

        // 覆盖确认：输出文件已存在时逐条询问，拒绝覆盖的跳过
        List<File> files = new ArrayList<>();
        for (File f : selected) {
            File out = opOutput(f, m, op);
            if (out != null && out.exists() && !confirmOverwrite(out)) continue;
            files.add(f);
        }
        if (files.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可处理的文件");
            return;
        }
        boolean del = deleteOriginal.isSelected();

        setBusy(true);
        progressBar.setValue(0);
        statusLabel.setText("处理中...");

        // 收集本次操作生成的输出文件，完成后自动加入列表
        final List<File> generated = Collections.synchronizedList(new ArrayList<>());
        SwingWorker<int[], String> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() {
                int ok = 0, fail = 0;
                for (int i = 0; i < files.size(); i++) {
                    File f = files.get(i);
                    try {
                        switch (op) {
                            case ENCRYPT: {
                                File out = new File(f.getAbsolutePath() + m.getEncryptedSuffix());
                                m.encrypt(f, out, ctx);
                                generated.add(out);
                                if (del && !out.getCanonicalPath().equals(f.getCanonicalPath()))
                                    Files.deleteIfExists(f.toPath());
                                break;
                            }
                            case DECRYPT: {
                                File out = m.getDecryptTarget(f);
                                m.decrypt(f, out, ctx);
                                generated.add(out);
                                if (del && !out.getCanonicalPath().equals(f.getCanonicalPath()))
                                    Files.deleteIfExists(f.toPath());
                                break;
                            }
                            case HASH: {
                                File out = m.getHashTarget(f);
                                m.hash(f, out, ctx);
                                generated.add(out);
                                break;
                            }
                            case SIGN: {
                                File out = m.getSignatureTarget(f);
                                m.sign(f, out, ctx);
                                generated.add(out);
                                break;
                            }
                            case VERIFY: {
                                boolean pass = m.verify(f, null, ctx);
                                publish((pass ? "✔ 验证通过：" : "✘ 验证失败：") + f.getName());
                                ok++;
                                continue;
                            }
                        }
                        ok++;
                        publish("成功：" + f.getName());
                    } catch (Exception ex) {
                        fail++;
                        publish("失败：" + f.getName() + " → " + ex.getMessage());
                    }
                    setProgress((i + 1) * 100 / files.size());
                }
                return new int[]{ok, fail};
            }

            @Override
            protected void process(List<String> chunks) {
                statusLabel.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                setBusy(false);
                progressBar.setValue(100);
                try {
                    int[] r = get();
                    statusLabel.setText("完成：成功 " + r[0] + "，失败 " + r[1]);
                } catch (Exception ex) {
                    statusLabel.setText("执行中断：" + ex.getMessage());
                }
                // 把生成的输出文件加入列表，方便直接进行下一步（如加密后直接解密）
                synchronized (generated) {
                    for (File g : generated) addFile(g);
                }
            }
        };

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName()))
                progressBar.setValue((Integer) evt.getNewValue());
        });
        worker.execute();
    }

    /** 计算本次操作对应的输出文件；VERIFY 无输出文件，返回 null */
    private File opOutput(File f, CryptoMethod m, Operation op) {
        switch (op) {
            case ENCRYPT: return new File(f.getAbsolutePath() + m.getEncryptedSuffix());
            case DECRYPT: return m.getDecryptTarget(f);
            case HASH:    return m.getHashTarget(f);
            case SIGN:    return m.getSignatureTarget(f);
            default:      return null;
        }
    }

    private boolean confirmOverwrite(File out) {
        int r = JOptionPane.showConfirmDialog(this,
                "输出文件已存在，是否覆盖？\n" + out.getAbsolutePath(),
                "覆盖确认", JOptionPane.YES_NO_OPTION);
        return r == JOptionPane.YES_OPTION;
    }

    private void setBusy(boolean busy) {
        categoryBox.setEnabled(!busy);
        methodBox.setEnabled(!busy);
        if (!busy) updateButtons();
        else {
            encryptBtn.setEnabled(false);
            decryptBtn.setEnabled(false);
            hashBtn.setEnabled(false);
            signBtn.setEnabled(false);
            verifyBtn.setEnabled(false);
        }
    }
}