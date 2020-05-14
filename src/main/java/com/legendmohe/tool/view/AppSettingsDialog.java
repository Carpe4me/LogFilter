package com.legendmohe.tool.view;

import com.legendmohe.tool.config.AppSettings;
import com.legendmohe.tool.config.ThemeConstant;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Created by xinyu.he on 2016/2/5.
 */
public class AppSettingsDialog extends JDialog {

    public static final int SETTINGS_WINDOW_WIDTH = 500;

    public AppSettingsDialog(Frame frame) {
        super(frame, "Settings", true);

        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.setPreferredSize(new Dimension(SETTINGS_WINDOW_WIDTH, 400));

        JPanel mainPanel = createMainPanel();
        JScrollPane jsp = new JScrollPane(mainPanel);
        c.add(jsp, BorderLayout.CENTER);

        this.pack();
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        Component themeSettingsPanel = createThemeSettingsPanel();
        mainPanel.add(themeSettingsPanel);

        return mainPanel;
    }

    private Component createThemeSettingsPanel() {
        JPanel themePanel = new JPanel(new BorderLayout()){
            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        themePanel.setPreferredSize(new Dimension(SETTINGS_WINDOW_WIDTH - 30, 50));
        themePanel.setBorder(BorderFactory.createTitledBorder("theme"));

        JLabel themeName = new JLabel("theme:");
        themePanel.add(themeName, BorderLayout.WEST);

        JComboBox<String> themeNameCombo = new JComboBox<>();

        themeNameCombo.addItem(ThemeConstant.THEME_NAME_DEFAULT);
        themeNameCombo.addItem(ThemeConstant.THEME_NAME_LIGHT);
        themeNameCombo.addItem(ThemeConstant.THEME_NAME_DARK);

        for (int i = 0; i < themeNameCombo.getItemCount(); i++) {
            if (themeNameCombo.getItemAt(i).equals(AppSettings.getTheme())) {
                themeNameCombo.setSelectedIndex(i);
            }
        }
        themeNameCombo.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED)
                return;
            String selected = (String) e.getItem();
            changeTheme(selected);
        });
        themePanel.add(themeNameCombo, BorderLayout.CENTER);

        return themePanel;
    }

    private void changeTheme(String theme) {
        AppSettings.setTheme(theme);
    }

}