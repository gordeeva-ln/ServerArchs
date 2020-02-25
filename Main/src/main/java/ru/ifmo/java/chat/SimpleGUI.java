/*Этот класс соответствует графической части приложения
* чтобы принять на вход необходимые данные и отрисовать графики
* входные данные - считаем для простоты натуральными числами (в том числе временной промежуток)
*
* Этот код был написан на основе туториала, поэтому часть переменных названы так, как было там*/

package ru.ifmo.java.chat;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import javax.swing.*;


public class SimpleGUI extends JFrame{
    private JButton button = new JButton("Start");

    private JLabel labelA = new JLabel("Choose architecture");

    private JRadioButton architecture1 = new JRadioButton("1");
    private JRadioButton architecture2 = new JRadioButton("2");
    private JRadioButton architecture3 = new JRadioButton("3");

    private JLabel labelX = new JLabel("Input X value (int):");
    private JTextField xValue = new JTextField("10", 5);

    private JLabel labelP = new JLabel("Choose parameter");
    private JRadioButton parameter1 = new JRadioButton("N");
    private JRadioButton parameter2 = new JRadioButton("M");
    private JRadioButton parameter3 = new JRadioButton("delta");

    private JLabel labelMin = new JLabel("Input min parameter value:");
    private JTextField minValue = new JTextField("1", 5);
    private JLabel labelMax = new JLabel("Input max parameter value:");
    private JTextField maxValue = new JTextField("6", 5);
    private JLabel labelStep = new JLabel("Input step for parameter:");
    private JTextField stepValue = new JTextField("1", 5);

    private JLabel labelN = new JLabel("Input N:");
    private JTextField nValue = new JTextField("10", 5);
    private JLabel labelM = new JLabel("Input M:");
    private JTextField mValue = new JTextField("10", 5);
    private JLabel labelD = new JLabel("Input delta:");
    private JTextField deltaValue = new JTextField("10", 5);

    private JCheckBox check = new JCheckBox("Check", false);

    public SimpleGUI() {
        super("Simple Example");
        this.setBounds(100, 100, 500, 200);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container container = this.getContentPane();
        container.setLayout(new GridLayout(3, 5, 4, 4));

        ButtonGroup architecture = new ButtonGroup();
        architecture.add(architecture1);
        architecture.add(architecture2);
        architecture.add(architecture3);

        container.add(labelA);
        container.add(architecture1);
        architecture1.setSelected(true);
        container.add(architecture2);
        container.add(architecture3);

        container.add(labelX);
        container.add(xValue);

        ButtonGroup param = new ButtonGroup();
        param.add(parameter1);
        param.add(parameter2);
        param.add(parameter3);

        container.add(labelP);
        container.add(parameter1);
        parameter1.setSelected(true);
        container.add(parameter2);
        container.add(parameter3);

        container.add(labelMin);
        container.add(minValue);
        container.add(labelMax);
        container.add(maxValue);
        container.add(labelStep);
        container.add(stepValue);

        container.add(labelN);
        container.add(nValue);
        container.add(labelM);
        container.add(mValue);
        container.add(labelD);
        container.add(deltaValue);

        button.addActionListener(new ButtonList());
        container.add(button);
    }

    class ButtonList implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            // validate input (at all)
            String errorMessage = "";
            int x;
            int min;
            int max;
            int step;
            int n;
            int m;
            int delta;
            try {
                x = Integer.parseInt(xValue.getText());
                min = Integer.parseInt(minValue.getText());
                max = Integer.parseInt(maxValue.getText());
                if (min > max) {
                    errorMessage += "Min value for parameter is greater than max value!\n";
                    return;
                }
                step = Integer.parseInt(stepValue.getText());
                if (step == 0) {
                    errorMessage += "Step value is zero!\n";
                    return;
                }

                n = Integer.parseInt(nValue.getText());
                m = Integer.parseInt(mValue.getText());
                delta = Integer.parseInt(deltaValue.getText());
                //System.out.println("Delta " + delta);
            } catch (NumberFormatException e) {
                errorMessage += e.getMessage() + "\n";
                return;
            } finally {
                if (!errorMessage.equals("")) {
                    JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.PLAIN_MESSAGE);

                }
            }
            int ar = (architecture1.isSelected() ? 1 : (architecture2.isSelected() ? 2 : 3));
            char param = (parameter1.isSelected() ? 'N' : (parameter2.isSelected() ? 'M' : 'D'));

            try {
                Map<Integer, Double[]> result = new App().run(x, min, max, step, n, m, delta,
                        ar,
                        param);
                drawPlots(result, min, max, step);

                writeFiles(result, x, min, max, step, n, m, delta, ar, param);
            }catch (IOException | InterruptedException e) {
                System.out.println("Some problems while client or server work\n" + e);
            }

        }
    }

    class GraphTimeReq extends JComponent {
        Double[] points;
        int min, max, step, count;
        String[] xlabs;
        String title;
        public GraphTimeReq(Double[] points, int min, int max, int step, String title) {
            this.points = points;
            this.min = min;
            this.max = max;
            this.step = step;
            this.title = title;
            this.count = (this.max - this.min) / this.step + 1;
            xlabs = new String[count];
            for (int i = 0; i < count; i++) xlabs[i] = Integer.valueOf(this.min + this.step * i).toString();
        }

        public Dimension getPreferredSize() {
            return new Dimension(550, 500);
        }

        public void paintComponent(Graphics g) {
            int minx = 50;
            int maxx = 520;
            int miny = 450;
            int maxy = 50;

            Graphics2D g2 = (Graphics2D) g;
            g2.setFont(new Font("Arial", Font.ITALIC, 18));
            g2.drawString(title, 150, 20);
            g2.drawLine(minx - 50, miny, maxx, miny); //x
            g2.drawLine(minx, maxy - 50, minx, miny + 50); //y
            g2.setFont(new Font("Arial", Font.PLAIN, 14));

            int stepx = (maxx - minx) / (count + 1);
            for (int i = 0; i < count; i++) {
                int ix = (i + 1) * stepx + minx;
                g2.drawLine(ix, miny - 10, ix, miny + 10);
                g2.drawString(xlabs[i], ix - 10, miny + 20);
            }

            double scale = (miny - maxy) / Collections.max(Arrays.asList(points));

            for (int i = 0; i < count; i++) {

                int ix0 = (i + 1) * stepx + minx;
                int iy0 = miny - (int) (points[i] * scale);
                g2.drawRect(ix0 - 2, iy0 - 2, 3, 3);

                if (i < count -1) {
                    int ix1 = (i + 2) * stepx + minx;
                    int iy1 = miny - (int) (points[i + 1] * scale);
                    g2.drawLine(ix0, iy0, ix1, iy1);
                }

            }

        }
    }
    class GraphFrame extends JFrame {
        GraphFrame(Map<Integer, Double[]> result, int min, int max, int step) {
            setLayout(new FlowLayout());
            add(new GraphTimeReq(result.get(1), min, max, step, "On Server"));
            add(new GraphTimeReq(result.get(2), min, max, step, "Client on Server"));
            add(new GraphTimeReq(result.get(3), min, max, step, "On Client"));
            pack();
        }
    }
    private void drawPlots(Map<Integer, Double[]> result, int min, int max, int step) {
        GraphFrame frame = new GraphFrame(result, min, max, step);
        frame.setTitle("Graphics");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }

    private void writeFiles(Map<Integer, Double[]> result, int x,
                            int min, int max, int step, int n, int m, int delta, int ar, char param) {
        for (int i : result.keySet()) {
            //System.out.println(i);
            try (FileWriter writer = new FileWriter("results/res_" + ar + "_" + param + "_" + i + ".txt", false)) {
                // write info in first line with spaces
                writer.write(x + " " + min + " " + max + " " + step + " " + n + " " + m + " " + delta + '\n');
                // write values
                Double[] values = result.get(i);
                for (Double d : values) {
                    //System.out.println("v " + d);
                    writer.write(d.toString() + '\n');
                }
                writer.flush();
            } catch (IOException e) {
                System.out.println("File have problems: " + e.getMessage());
            }
        }
    }
}
