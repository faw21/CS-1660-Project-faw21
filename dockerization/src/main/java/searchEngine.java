import javax.swing.* ;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import com.google.cloud.storage.Blob;

public class searchEngine extends JFrame{
    private JPanel panel;

    private JButton chooseFileButton;
    private JButton loadEngineButton;
    private JFileChooser fileChooser;
    private ArrayList<File> fileList = new ArrayList<File>();
    private JTextArea files;
    private JScrollPane scrollPane;
    private JTextArea invertedindex;
    private JScrollPane resultPane;
    private ArrayList<Blob> results = new ArrayList<Blob>();
    private JLabel engineloaded = new JLabel("Engine Loaded!");
    private JLabel invertedIndexConstructed = new JLabel("Inverted Indices Constructed and Shown Below:");

    public static void main(String[] args){
        new searchEngine();
    }

    public searchEngine(){
        setDefaultLookAndFeelDecorated(true);

        setTitle("Super-fancy Cloud Computing Interface");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setSize(500,500);

        buildPanel();

        add(panel);

        setVisible(true);
    }

    private void buildPanel(){
        panel = new JPanel();
        panel.setLayout(null);

        files = new JTextArea("File selected:");
        scrollPane = new JScrollPane(files);
        scrollPane.setBounds(100, 100, 300, 60);
        panel.add(scrollPane);
        chooseFileButton = new JButton("choose files");
        chooseFileButton.setBounds(150, 50, 200, 40);
        chooseFileButton.addActionListener(new fileChooseListener());
        panel.add(chooseFileButton);
        loadEngineButton = new JButton("Load Engine");
        loadEngineButton.setBounds(150, 260, 200, 40);
        loadEngineButton.addActionListener(new loadEngineListener());
        panel.add(loadEngineButton);
        invertedindex = new JTextArea();
        resultPane = new JScrollPane(invertedindex);
        resultPane.setBounds(20, 100, 460, 350);
        resultPane.setVisible(false);
        panel.add(resultPane);
        engineloaded.setBounds(200,30,100,25);
        engineloaded.setVisible(false);
        panel.add(engineloaded);
        invertedIndexConstructed.setBounds(100,60,300,25);
        invertedIndexConstructed.setVisible(false);
        panel.add(invertedIndexConstructed);


    }

    private class fileChooseListener implements ActionListener{
        public void actionPerformed(ActionEvent event) {
            fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("/User/wufangzheng"));
            int result = fileChooser.showOpenDialog(new JFrame());


            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                fileList.add(selectedFile);
                JOptionPane.showMessageDialog(getComponent(0), "Selected file: " + selectedFile.getAbsolutePath());
                files.setText(files.getText() + '\n' + selectedFile.getAbsolutePath());
            }
        }
    }

    private class loadEngineListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            String projectId = "corded-sunbeam-273920";
            // project-id of project to create the cluster in
            String region = "us-east4";  // region to create the cluster
            String clusterName = "cs1660-project-cluster"; // name of the cluster
            String jobFilePath = "gs://dataproc-staging-us-east4-9371974370-akh5vmt7"; // location in GCS of the PySpark job
            try {
                results = Request.request(projectId, region, clusterName, jobFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            chooseFileButton.setVisible(false);
            loadEngineButton.setVisible(false);
            scrollPane.setVisible(false);
            resultPane.setVisible(true);
            invertedindex.setText(new String(results.get(0).getContent()));
            engineloaded.setVisible(true);
            invertedIndexConstructed.setVisible(true);


        }
    }

}
