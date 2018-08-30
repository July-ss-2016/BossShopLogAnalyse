package vip.ourcraft.programs.bossshoploganalyse;

import com.google.gson.Gson;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainWindow {
    private static final String[] XLSX_HEADERS = new String[] {"交易总金额", "交易总次数", "交易总人数"};
    private JFrame frame;
    private JPanel panel;
    private JLabel logFilePathLabel;
    private JButton runButton;
    private JLabel priceFilePathLabel;
    private JTextField priceFileTextField;
    private JTextField logFilePathTextField;
    private JButton cleanOldAnalysesButton;

    MainWindow() {
        runButton.addActionListener(e -> {
            if (priceFileTextField.getText().equals("")) {
                sendWarningMsgBox("价格文件路径不能为空!");
                return;
            }

            if (logFilePathTextField.getText().equals("")) {
                sendWarningMsgBox("日志文件路径不能为空!");
                return;
            }

            output();
        });
        
        // 实现拖拽路径
        registerTextFieldDropTarget(logFilePathTextField);
        registerTextFieldDropTarget(priceFileTextField);
    }
    
    private void registerTextFieldDropTarget(JTextField textField) {
        // 文件路径接收器
        new DropTarget(textField, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    // 接收拖拽来的数据
                    event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

                    try {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) (event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));

                        textField.setText(files.get(0).getAbsolutePath());
                        event.dropComplete(true);
                    } catch (UnsupportedFlavorException | IOException e) {
                        e.printStackTrace();
                    }

                    return;
                }

                event.rejectDrop();
            }
        });
    }

    public void init() {
        this.frame = new JFrame("BossShopLogAnalyse");

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(500,200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void output() {
        File priceFile = new File(priceFileTextField.getText());
        File logFile = new File(logFilePathTextField.getText());

        if (!priceFile.exists()) {
            sendWarningMsgBox("价格文件不存在!");
            return;
        }

        if (!logFile.exists()) {
            sendWarningMsgBox("日志文件不存在!");
            return;
        }

        Workbook priceWorkook = null;

        try {
            priceWorkook = new XSSFWorkbook(new BufferedInputStream(new FileInputStream(priceFile), 2048)); // 不知道为什么不能用File作为参数, FileInputStream可行
        } catch (IOException e) {
            sendErrorMsgBox(e.getLocalizedMessage());
            System.out.println(e.getMessage());
        }

        if (priceWorkook == null || priceWorkook.getNumberOfSheets() == 0) {
            sendWarningMsgBox("没有找到数据表!");
            return;
        }

        Sheet priceWorkookSheet = priceWorkook.getSheetAt(0);
        Row headerRow = priceWorkookSheet.getRow(0);
        int firstAnalyseHeader = headerRow.getPhysicalNumberOfCells();

        // 设置统计表头
        for (int i = 0; i < XLSX_HEADERS.length; i++) {
            int index = firstAnalyseHeader + i;
            Cell cell = headerRow.getCell(index);

            if (cell == null) {
                cell = headerRow.createCell(index);
            }

            cell.setCellValue("BSLA-" + logFile.getName() + "-" + XLSX_HEADERS[i]);
        }

        String logLines = Util.readFile(logFile);

        if (logLines == null) {
            sendWarningMsgBox("日志文件为空!");
            return;
        }

        HashMap<String, BossShopLogAnalyse> bsLogAnalyses = new HashMap<>();

        // 逐行统计
        for (String line : logLines.split(Util.LINE_SEPARATOR)) {
            Gson gson = new Gson();
            BossShopLog bossShopLog = gson.fromJson(line.substring(10), BossShopLog.class);
            String itemIndex = bossShopLog.getShopName() + ":" + bossShopLog.getItemName();
            String player = bossShopLog.getPlayerName();
            boolean isSellShop = bossShopLog.getReward().startsWith("[");

            // 不存在统计类则创建
            if (!bsLogAnalyses.containsKey(itemIndex)) {
                BossShopLogAnalyse temp = new BossShopLogAnalyse();

                bsLogAnalyses.put(itemIndex, temp);
                temp.setItemIndex(itemIndex);
                temp.setItemInfo(isSellShop ? bossShopLog.getReward() : bossShopLog.getPrice());
                temp.setSourceFile(logFile);
            }

            // 得到统计结果，继续统计
            BossShopLogAnalyse logAnalyse = bsLogAnalyses.get(itemIndex);

            // 递增总价
            logAnalyse.addTotalTradedPrice(Double.parseDouble(isSellShop ? bossShopLog.getPrice() : bossShopLog.getReward()));
            // 递增交易次数
            logAnalyse.addTotalTradedCount(1);
            // 添加交易过的玩家id
            logAnalyse.putTradedPlayer(player);
        }

        HashMap<String, Row> firstColumnRows = new HashMap<>();

        // 把第一列对应的物品索引和表格索引存储起来
        for (int i = 1; i < priceWorkookSheet.getPhysicalNumberOfRows(); i++) {
            Cell cell = priceWorkookSheet.getRow(i).getCell(0);

            if (cell == null) {
                continue;
            }

            String value = String.valueOf(priceWorkookSheet.getRow(i).getCell(0).getNumericCellValue());
            int tmp = value.indexOf(".");

            firstColumnRows.put(priceWorkookSheet.getSheetName() + ":" + value.substring(0, tmp == -1 ? value.length() : tmp), priceWorkookSheet.getRow(i));
        }

        for (Map.Entry<String, BossShopLogAnalyse> entry : bsLogAnalyses.entrySet()) {
            BossShopLogAnalyse analyse = entry.getValue();
            // 在excel中物品索引没有i作为前缀
            String itemIndex = entry.getKey().replace("i", "");

            if (firstColumnRows.containsKey(itemIndex)) {
                Row row = firstColumnRows.get(itemIndex);

                row.createCell(8).setCellValue(Util.halfDownDouble(analyse.getTotalTradedPrice()) + "(" + Util.halfDownDouble(analyse.getPercentOfGlobalTradedPrice()) * 100 + "%)");
                row.createCell(9).setCellValue(analyse.getTotalTradedCount() + "(" + Util.halfDownDouble(analyse.getPercentOfGlobalTradedCount()) * 100 + "%)");
                row.createCell(10).setCellValue(analyse.getTradedPlayerCount() + "(" + Util.halfDownDouble(analyse.getPercentOfGlobalTradedPlayerCount()) * 100 + "%)");
            }
        }

        for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
            priceWorkookSheet.autoSizeColumn(i);
        }

        try {
            priceWorkook.write(new FileOutputStream(priceFile));
        } catch (IOException e) {
            e.printStackTrace();
            sendErrorMsgBox(e.getMessage());
        }

        sendInfoMsgBox("更新文件成功!");
    }



    private void sendInfoMsgBox(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "警告", JOptionPane.INFORMATION_MESSAGE);
    }

    private void sendWarningMsgBox(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "警告", JOptionPane.WARNING_MESSAGE);
    }

    private void sendErrorMsgBox(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "错误", JOptionPane.ERROR_MESSAGE);
    }
}
