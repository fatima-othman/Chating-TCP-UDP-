
package network_project;
import java.util.Timer;
import java.util.TimerTask;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.awt.Color;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class p1 extends javax.swing.JFrame {
    private Map<String, Long> archivedMessagesMap = new HashMap<>();
    private static Integer Order = 1;
    private final int RECEIVED = 0;
    private final int SENT = 1; 
    
    File file = new File("Recordes.txt");
    
    Thread t ;
    DatagramSocket Socket;
    String SendMessage;
    boolean firstClick = true;
     



    private void updateChatTextArea(String replacementText, int lineNumber, int side) {
        // Get the document of the text pane
        lineNumber--;
        StyledDocument doc = Chat.getStyledDocument();
        Element root = doc.getDefaultRootElement();

        // Ensure the line number is within bounds
        if (lineNumber >= 0 && lineNumber < root.getElementCount()) {
            // Get the element representing the specified line
            Element lineElement = root.getElement(lineNumber);

            try {
                // Get the start and end offsets of the line
                int startOffset = lineElement.getStartOffset();
                int endOffset = lineElement.getEndOffset() - 1; // Exclude newline character
                Style style = Chat.addStyle("", null);
                StyleConstants.setBold(style, true);
                StyleConstants.setFontSize(style, 12);
                if(side == RECEIVED){
                    StyleConstants.setForeground(style, Color.orange);
                }
                else{
                   StyleConstants.setForeground(style, Color.YELLOW);
                }
                // Replace the text in the specified range
                doc.remove(startOffset, endOffset - startOffset);
                doc.insertString(startOffset, replacementText, style);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        } else {
            System.err.println("Line number out of bounds.");
        }
            
    }

    private boolean isReceivedMessage(String selectedText) {
        if(selectedText.isEmpty()) {return true;}
        selectedText = selectedText.replaceAll("\\s", "");
        int dotIndex = selectedText.indexOf(".");
        if (dotIndex == -1){return true;}
        return selectedText.charAt(dotIndex+1) == 'R';
        
        
        
    }
   String replaceMeWithReceived(String Text){
        return Text.replace(" Me", " Recieved ");

    }

    private void updateMessageOnReceiverSide(String ReceiveMsg) {
        
        int line = getLineNumber(ReceiveMsg);
        
        updateChatTextArea(replaceMeWithReceived(ReceiveMsg), line, RECEIVED);
        
    }
    class P2PCon implements Runnable
    {
        @Override
        public void run() {
            Server(); 
        }

    }
    public p1() {
        initComponents();
        if(jComboBox1.getSelectedItem() == "Wi-Fi")
        {
            try {
                LocalIP.setText(InetAddress.getLocalHost().getHostAddress().toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        ArchiveCleanupThread archiveCleanupThread = new ArchiveCleanupThread();
        archiveCleanupThread.start();
        
    }
    
     private void updateArchivedTextArea() {
        StringBuilder newText = new StringBuilder();
        for (String message : archivedMessagesMap.keySet()) {
            newText.append(message).append("\n");
        }
        ArchivedMessagesTextArea.setText(newText.toString());
    }
     
    private class ArchiveCleanupThread extends Thread {//////////////////////////////////////////////////////////////inner class
        private volatile boolean running;

        public ArchiveCleanupThread() {
            this.running = true;
        }

        public void stopThread() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                FileWriter fw = null;
                try {
                    long currentTime = System.currentTimeMillis();
                    synchronized (archivedMessagesMap) {
                        Iterator<Map.Entry<String, Long>> iterator = archivedMessagesMap.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<String, Long> entry = iterator.next();
                            if (currentTime - entry.getValue() >= 120000) { // Check if message is older than 2 minutes
                                // Remove message from archivedMessagesMap
                                iterator.remove();
                                updateArchivedTextArea();
                                
                            }
                        }
                    }
                    fw = new FileWriter(file, true);
                    BufferedWriter writer = new BufferedWriter(fw);
                    
                    // Sleep for some time before checking again
                    try {
                        Thread.sleep(100); // Check every 10 seconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(p1.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        fw.close();
                    } catch (IOException ex) {
                        Logger.getLogger(p1.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }
    
    private void sendMessage(String messageToBeDeleted) {
    try {
        // Extract the destination IP and port from user input
        String[] ipdest = RemoteIP.getText().split("\\.");
        byte[] IP_other_device2 = {(byte) Integer.parseInt(ipdest[0]), (byte) Integer.parseInt(ipdest[1]), (byte) Integer.parseInt(ipdest[2]), (byte) Integer.parseInt(ipdest[3])};
        InetAddress IPDest = InetAddress.getByAddress(IP_other_device2);
        int remotePort = Integer.parseInt(RemotePort.getText());

        // Construct the delete message notification
        byte[] sendData = messageToBeDeleted.getBytes();

        // Send the delete message notification to the receiver
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPDest, remotePort);
        Socket.send(sendPacket);

        // Update the UI or perform other actions as needed
        Status.setText("sent a signal to" + IPDest.getHostAddress() + ", Port: " + remotePort);
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Please enter all fields correctly", "WARNING", JOptionPane.WARNING_MESSAGE);
    } catch (IllegalArgumentException e) {
        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Please enter remote port correctly", "WARNING", JOptionPane.WARNING_MESSAGE);
    } catch (ArrayIndexOutOfBoundsException e) {
        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Please enter IP address correctly", "WARNING", JOptionPane.WARNING_MESSAGE);
    } catch (NullPointerException e) {
        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Please start listening before sending message", "WARNING", JOptionPane.WARNING_MESSAGE);
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    
    void Client(){
        
            try
            {
            String [] ipdest=RemoteIP.getText().split("\\.");
            byte []IP_other_device={(byte)Integer.parseInt(ipdest[0]),(byte)Integer.parseInt(ipdest[1]),(byte)Integer.parseInt(ipdest[2]),(byte)Integer.parseInt(ipdest[3])};
            InetAddress IPDest = InetAddress.getByAddress(IP_other_device);

            LocalDateTime now = LocalDateTime.now();
            SendMessage = " ["+now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss a"))+"] :  "+SendMessageTextField.getText();
           
            System.out.println("The file has been created at: " + file.getAbsolutePath());
            byte[] SendData= SendMessage.getBytes();
            DatagramPacket SendPacket = new DatagramPacket(SendData, SendData.length, IPDest , Integer.parseInt(RemotePort.getText()));
            Socket.send(SendPacket); 
            String senmsg=Order + ". Me "+SendMessage+" from "+Socket.getLocalPort()+"\n";
            
            StyledDocument doc =Chat.getStyledDocument();
            Style s =Chat.addStyle("", null);
            StyleConstants.setForeground(s, Color.YELLOW);
            StyleConstants.setBold(s, true);
            StyleConstants.setFontSize(s, 12);
            doc.insertString(doc.getLength(), senmsg, s);
            Status.setText("Send to: "+SendPacket.getAddress().getHostAddress()+", Port: "+SendPacket.getPort());
        }catch(java.lang.NumberFormatException e)
        {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Please enter all fields correctly","WARNING", JOptionPane.WARNING_MESSAGE);
        }catch(java.lang.IllegalArgumentException e)
        {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Please enter remote port correctly","WARNING", JOptionPane.WARNING_MESSAGE);
        }catch(java.lang.ArrayIndexOutOfBoundsException e)
        {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Please enter IP address correctly","WARNING", JOptionPane.WARNING_MESSAGE);
        }catch(java.lang.NullPointerException e)
        {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Please start lisning before send message","WARNING", JOptionPane.WARNING_MESSAGE);
        }catch (Exception e)
        {
            e.printStackTrace();
        }         
           
            
            
        
       
    }
    int getLineNumber(String text){
        int dotIndex = text.indexOf('.');
        String numberString = text.substring(0, dotIndex);
        return Integer.parseInt(numberString);
    }
    
    void deleteMessageFromReceiverSide(String MessageToBeDeleted){
        int line = getLineNumber(MessageToBeDeleted);
        updateChatTextArea("[Message Deleted]", line, RECEIVED);
        
    }
    public int getLineCount() {
        Document doc = Chat.getDocument();
        int lineCount = 1; // At least one line
        try {
            int length = doc.getLength();
            for (int i = 0; i < length; i++) {
                if (doc.getText(i, 1).equals("\n")) {
                    lineCount++;
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return lineCount;
    }
    
    void Server(){
        try
        { 
            while(true){
                
                byte[] ReceiveData = new byte[65536];
                DatagramPacket ReceivePacket = new DatagramPacket(ReceiveData, ReceiveData.length);
                Socket.receive(ReceivePacket);// from other client
                String ReceiveMsg = new String(ReceivePacket.getData());
                ReceiveMsg = ReceiveMsg.trim();
               
        
                
                if(ReceiveMsg.charAt(0) == '$'){
                    ReceiveMsg = ReceiveMsg.substring(1);
                    deleteMessageFromReceiverSide(ReceiveMsg);
                   
                }
                else if(ReceiveMsg.charAt(0) == '&'){
                    ReceiveMsg = ReceiveMsg.substring(1);
                    updateMessageOnReceiverSide(ReceiveMsg);

                }
                else if(ReceiveMsg.equals("a")){
                    
                }
                else{
                Order = getLineCount();
                String sert = Order + ". Recieved"+ ReceiveMsg.trim() +" from "+ReceivePacket.getPort() +"\n";
                StyledDocument doc = Chat.getStyledDocument();
                Style style =Chat.addStyle("", null);
                StyleConstants.setForeground(style, Color.orange);
                StyleConstants.setBold(style, true); 
                StyleConstants.setFontSize(style, 12);
                doc.insertString(doc.getLength(), sert, style);
                Status.setText(Order + ". Recived from:"+ReceivePacket.getAddress().getHostAddress()+",Port:"+ReceivePacket.getPort());//ServerSocket.getLocalPort()
                File filee = new File("Logrec.txt");
                FileWriter writer = new FileWriter(file, true);
            
            // Append or create the file and write to it.
                writer.append("The Messege Has been"+ ". Recived from:"+ReceivePacket.getAddress().getHostAddress()+",Port:"+ReceivePacket.getPort()+" The Massegd has been sent :"+ Order + sert +"\n");
                writer.append("\n");
              
            // Always close the writer to avoid memory leaks.
                writer.close();
                }
            }
        }catch(java.lang.NumberFormatException e)
        {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Please enter correct local port ","WARNING", JOptionPane.WARNING_MESSAGE);
        }catch(java.net.BindException e)
        {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "port already used, please choose diffrent ","WARNING", JOptionPane.WARNING_MESSAGE);
        }catch (Exception e)
        {
           e.printStackTrace();
        }
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel3 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        LocalIP = new javax.swing.JTextField();
        LocalPort = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        RestoreArchivedMessageButton = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        RemoteIP = new javax.swing.JTextField();
        RemotePort = new javax.swing.JTextField();
        StartListing = new javax.swing.JButton();
        TestButton = new javax.swing.JButton();
        DeleteMessageButton = new javax.swing.JButton();
        DeleteConvoButton = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        ArchivedMessagesTextArea = new javax.swing.JTextArea();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        Chat = new javax.swing.JTextPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        SendMessageTextField = new javax.swing.JTextArea();
        Send = new javax.swing.JButton();
        Status = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();

        jLabel3.setBackground(java.awt.Color.green);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Client Chat");
        setBackground(new java.awt.Color(0, 51, 102));
        setMinimumSize(new java.awt.Dimension(1015, 460));
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(102, 102, 255));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Wi-Fi", "Ethernet", "Loopback pseudo-Interface" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });
        jPanel1.add(jComboBox1, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 10, 373, 31));

        jLabel4.setFont(new java.awt.Font("Baskerville Old Face", 1, 16)); // NOI18N
        jLabel4.setText("Available Interfaces");
        jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 10, 140, 30));

        LocalIP.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        LocalIP.setPreferredSize(new java.awt.Dimension(7, 24));
        LocalIP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LocalIPActionPerformed(evt);
            }
        });
        jPanel1.add(LocalIP, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 110, 205, -1));

        LocalPort.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        LocalPort.setPreferredSize(new java.awt.Dimension(7, 24));
        LocalPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LocalPortActionPerformed(evt);
            }
        });
        jPanel1.add(LocalPort, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 160, 207, -1));

        jLabel6.setFont(new java.awt.Font("Baskerville Old Face", 1, 18)); // NOI18N
        jLabel6.setText("Local Port:");
        jPanel1.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 160, -1, -1));

        jLabel5.setFont(new java.awt.Font("Baskerville Old Face", 1, 18)); // NOI18N
        jLabel5.setText("Local IP:");
        jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 110, -1, -1));

        RestoreArchivedMessageButton.setBackground(new java.awt.Color(153, 0, 153));
        RestoreArchivedMessageButton.setFont(new java.awt.Font("Baskerville Old Face", 0, 18)); // NOI18N
        RestoreArchivedMessageButton.setForeground(new java.awt.Color(255, 255, 255));
        RestoreArchivedMessageButton.setText("Restore Message");
        RestoreArchivedMessageButton.setToolTipText("");
        RestoreArchivedMessageButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        RestoreArchivedMessageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RestoreArchivedMessageButtonActionPerformed(evt);
            }
        });
        jPanel1.add(RestoreArchivedMessageButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(1010, 400, 150, 40));

        jLabel7.setFont(new java.awt.Font("Baskerville Old Face", 1, 18)); // NOI18N
        jLabel7.setText("Remote Port:");
        jPanel1.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 260, -1, 20));

        jLabel8.setFont(new java.awt.Font("Baskerville Old Face", 1, 18)); // NOI18N
        jLabel8.setText("Remote IP:");
        jPanel1.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 210, -1, -1));

        RemoteIP.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        RemoteIP.setPreferredSize(new java.awt.Dimension(7, 24));
        RemoteIP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemoteIPActionPerformed(evt);
            }
        });
        jPanel1.add(RemoteIP, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 210, 207, -1));

        RemotePort.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        RemotePort.setPreferredSize(new java.awt.Dimension(7, 24));
        RemotePort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemotePortActionPerformed(evt);
            }
        });
        jPanel1.add(RemotePort, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 260, 206, -1));

        StartListing.setBackground(new java.awt.Color(153, 0, 153));
        StartListing.setFont(new java.awt.Font("Baskerville Old Face", 0, 18)); // NOI18N
        StartListing.setForeground(new java.awt.Color(255, 255, 255));
        StartListing.setText("Start Listening ");
        StartListing.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        StartListing.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StartListingActionPerformed(evt);
            }
        });
        jPanel1.add(StartListing, new org.netbeans.lib.awtextra.AbsoluteConstraints(940, 20, 260, 30));

        TestButton.setBackground(new java.awt.Color(153, 0, 153));
        TestButton.setFont(new java.awt.Font("Baskerville Old Face", 0, 18)); // NOI18N
        TestButton.setForeground(new java.awt.Color(255, 255, 255));
        TestButton.setText("Test Button");
        TestButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        TestButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TestButtonActionPerformed(evt);
            }
        });
        jPanel1.add(TestButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(1010, 450, 150, 40));

        DeleteMessageButton.setBackground(new java.awt.Color(153, 0, 153));
        DeleteMessageButton.setFont(new java.awt.Font("Baskerville Old Face", 0, 18)); // NOI18N
        DeleteMessageButton.setForeground(new java.awt.Color(255, 255, 255));
        DeleteMessageButton.setText("Delete all");
        DeleteMessageButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        DeleteMessageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteMessageButtonActionPerformed(evt);
            }
        });
        jPanel1.add(DeleteMessageButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(1010, 350, 150, 41));

        DeleteConvoButton.setBackground(new java.awt.Color(153, 0, 153));
        DeleteConvoButton.setFont(new java.awt.Font("Baskerville Old Face", 0, 18)); // NOI18N
        DeleteConvoButton.setForeground(new java.awt.Color(255, 255, 255));
        DeleteConvoButton.setText("Delete last message");
        DeleteConvoButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        DeleteConvoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteConvoButtonActionPerformed(evt);
            }
        });
        jPanel1.add(DeleteConvoButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(1010, 300, 150, 41));

        jScrollPane3.setBorder(null);

        ArchivedMessagesTextArea.setEditable(false);
        ArchivedMessagesTextArea.setBackground(new java.awt.Color(255, 255, 255));
        ArchivedMessagesTextArea.setColumns(20);
        ArchivedMessagesTextArea.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        ArchivedMessagesTextArea.setRows(5);
        ArchivedMessagesTextArea.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, new java.awt.Color(255, 207, 174)));
        jScrollPane3.setViewportView(ArchivedMessagesTextArea);

        jPanel1.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 340, 320, 150));

        jLabel11.setFont(new java.awt.Font("Baskerville Old Face", 1, 18)); // NOI18N
        jLabel11.setText("Archive");
        jPanel1.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 320, -1, -1));

        Chat.setEditable(false);
        Chat.setBackground(new java.awt.Color(255, 255, 255));
        Chat.setBorder(null);
        Chat.setDisabledTextColor(new java.awt.Color(204, 204, 255));
        jScrollPane4.setViewportView(Chat);

        jPanel1.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 120, 515, 295));

        SendMessageTextField.setBackground(new java.awt.Color(242, 242, 242));
        SendMessageTextField.setColumns(20);
        SendMessageTextField.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        SendMessageTextField.setRows(5);
        jScrollPane2.setViewportView(SendMessageTextField);

        jPanel1.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(860, 100, 370, 140));

        Send.setBackground(new java.awt.Color(153, 0, 153));
        Send.setFont(new java.awt.Font("Baskerville Old Face", 0, 18)); // NOI18N
        Send.setForeground(new java.awt.Color(255, 255, 255));
        Send.setText("Send");
        Send.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        Send.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SendActionPerformed(evt);
            }
        });
        jPanel1.add(Send, new org.netbeans.lib.awtextra.AbsoluteConstraints(1010, 250, 150, 40));

        Status.setEditable(false);
        Status.setBackground(new java.awt.Color(255, 255, 255));
        Status.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        Status.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StatusActionPerformed(evt);
            }
        });
        jPanel1.add(Status, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 440, 464, 30));

        jLabel9.setFont(new java.awt.Font("Baskerville Old Face", 1, 18)); // NOI18N
        jLabel9.setText("Status:");
        jPanel1.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 450, -1, -1));

        jLabel1.setFont(new java.awt.Font("Baskerville Old Face", 1, 22)); // NOI18N
        jLabel1.setText("Your Chat");
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 70, 105, -1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 1236, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 508, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void LocalPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LocalPortActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_LocalPortActionPerformed

    private void RemotePortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RemotePortActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_RemotePortActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
       if(jComboBox1.getSelectedItem().equals("Loopback pseudo-Interface"))
       {
           LocalIP.setText("127.0.0.1");
           RemoteIP.setText("127.0.0.1");
       }
       else if(jComboBox1.getSelectedItem().equals("Wi-Fi"))
        {
            try {
                RemoteIP.setText("");
                LocalIP.setText(InetAddress.getLocalHost().getHostAddress().toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
       else
       {
           LocalIP.setText("");
           RemoteIP.setText("");
       }
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void StatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StatusActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_StatusActionPerformed
//    static  boolean flag=false; 
    private void SendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SendActionPerformed
        
        
        if(firstClick){
            sendMessage("a");
            firstClick = false;
        }
        else if(SendMessageTextField.getText().
                isEmpty()){
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Please enter a message","WARNING", JOptionPane.WARNING_MESSAGE);
        }
        else{
        Order = getLineCount();
        Client();
        SendMessageTextField.setText("");}
    }//GEN-LAST:event_SendActionPerformed

    private void TestButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TestButtonActionPerformed
        SendMessageTextField.setText("test");
         Order = getLineCount();
        Client();
        SendMessageTextField.setText("");
       
    }//GEN-LAST:event_TestButtonActionPerformed

    private void StartListingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StartListingActionPerformed
            
        try{
            if(Socket == null)
            {
                Socket =new DatagramSocket(Integer.parseInt(LocalPort.getText()));
                P2PCon conn =new P2PCon();
                t =new Thread(conn);
                t.start();
            }
            else
            {
                Socket.close();
               // t.stop();
                t.interrupt();
                Socket =new DatagramSocket(Integer.parseInt(LocalPort.getText()));
                P2PCon conn =new P2PCon();
                t =new Thread(conn);
                t.start();
            }
        }catch(java.lang.NumberFormatException e)
        {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Please enter Local IP and local port correctly","WARNING", JOptionPane.WARNING_MESSAGE);
        }catch(java.lang.IllegalArgumentException e)
        {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Please enter correct local port ","WARNING", JOptionPane.WARNING_MESSAGE);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
            
    }//GEN-LAST:event_StartListingActionPerformed

    private void DeleteMessageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteMessageButtonActionPerformed
        try {
        // Get selected text start and end positions
        int start = Chat.getSelectionStart();
        int end = Chat.getSelectionEnd();

        // Remove selected text from the document
        if (start != end) {
            String selectedText = Chat.getSelectedText();
            if (isReceivedMessage(selectedText)) {
                JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), 
                    "Can't Delete Recipient Message!", "WARNING", JOptionPane.WARNING_MESSAGE); 
            } else {
                // Store timestamp of archived message along with its content
                archivedMessagesMap.put(selectedText, System.currentTimeMillis());

                // Log the deletion event
                File file = new File("Logrec.txt");
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.append("This Message " + selectedText + " Has been Deleted by User who has IP: " 
                        + this.LocalIP.getText() + " and the User has port Number: " 
                        + this.LocalPort.getText() + "\n");
                } catch (IOException ex) {
                    Logger.getLogger(p1.class.getName()).log(Level.SEVERE, null, ex);
                }

                // Add selected text to ArchivedMessagesTextArea
                ArchivedMessagesTextArea.append(selectedText + "\n");
                System.out.println("This is text\n" + selectedText);

                // Remove selected text from the document
                Chat.getDocument().remove(start, end - start);
                Chat.getDocument().insertString(start, "[Message Deleted]", null);
                sendMessage("$" + selectedText);

                // Create a Timer to remove the message after 2 minutes
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        SwingUtilities.invokeLater(() -> {
                            String archivedText = ArchivedMessagesTextArea.getText();
                            int index = archivedText.indexOf(selectedText);
                            if (index != -1) {
                                ArchivedMessagesTextArea.setText(archivedText.substring(0, index) + 
                                    archivedText.substring(index + selectedText.length() + 1));
                            }
                        });
                    }
                };

               
                timer.schedule(task, 120000);
            }
        }
    } catch (BadLocationException ex) {
        ex.printStackTrace();
    }
    }//GEN-LAST:event_DeleteMessageButtonActionPerformed

    private void DeleteConvoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteConvoButtonActionPerformed
    StyledDocument doc = Chat.getStyledDocument();
    Element root = doc.getDefaultRootElement();

    try {
        // Iterate through each line in the text pane
        for (int i = 0; i < root.getElementCount(); i++) {
            Element lineElement = root.getElement(i);
            int startOffset = lineElement.getStartOffset();
            int endOffset = lineElement.getEndOffset();

            // Retrieve the text content of the line
            String lineText = doc.getText(startOffset, endOffset - startOffset);
            if (isReceivedMessage(lineText)) {
                continue;
            } else {
                // Store timestamp of archived message along with its content
                archivedMessagesMap.put(lineText, System.currentTimeMillis());

                // Add the line to ArchivedMessagesTextArea
                ArchivedMessagesTextArea.append(lineText + "\n");

                // Log the deletion event
                File file = new File("Logrec.txt");
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.append("This Message " + lineText + " Has been Deleted by User who has IP: " 
                        + this.LocalIP.getText() + " and the User has port Number: " 
                        + this.LocalPort.getText() + "\n");
                } catch (IOException ex) {
                    Logger.getLogger(p1.class.getName()).log(Level.SEVERE, null, ex);
                }

                // Replace the line in the ChatTextPane with "[Message Deleted]"
                doc.remove(startOffset, endOffset - startOffset);
                sendMessage("$" + lineText);

                // Create a Timer to remove the message from ArchivedMessagesTextArea after 2 minutes
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        SwingUtilities.invokeLater(() -> {
                            String archivedText = ArchivedMessagesTextArea.getText();
                            // Remove the message from ArchivedMessagesTextArea
                            String textToRemove = lineText + "\n";
                            int index = archivedText.indexOf(textToRemove);
                            if (index != -1) {
                                ArchivedMessagesTextArea.setText(archivedText.substring(0, index) + 
                                    archivedText.substring(index + textToRemove.length()));
                            }
                        });
                    }
                };

                
                timer.schedule(task, 120000);
            }
        }
    } catch (BadLocationException ex) {
        ex.printStackTrace();
    }
    }//GEN-LAST:event_DeleteConvoButtonActionPerformed

    private void RestoreArchivedMessageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RestoreArchivedMessageButtonActionPerformed
                try {
                    // Get selected text start and end positions
                    int start = ArchivedMessagesTextArea.getSelectionStart();
                    int end = ArchivedMessagesTextArea.getSelectionEnd();
                     // Retrieve selected text
//                    String selectedText = p1.getText().substring(start, end);

                    // Add selected text to ArchivedMessagesTextArea
//                    ArchivedMessagesTextArea.append(selectedText + "\n");

                    // Remove selected text from the document
                    if (start != end) {
                        String selectedText = ArchivedMessagesTextArea.getText().substring(start, end);
                        deleteMessageFromMap(selectedText);
                        int line = getLineNumber(selectedText);
                        ArchivedMessagesTextArea.getDocument().remove(start, end - start);
                        ArchivedMessagesTextArea.getDocument().insertString(start, "", null);
                        // Remove selected text from the document
                        updateChatTextArea(selectedText, line, SENT);
                        sendMessage("&"+selectedText);
                         File filee = new File("Logrec.txt");
                try {
                    FileWriter writer = new FileWriter(file, true);
                    writer.append("This Message"+selectedText+" Has been Restored by User who has ip: "+this.LocalIP.getText()+" and the User has port Nmnber :"+this.LocalPort.getText()+ "\n");
                    writer.close();
                    } catch (IOException ex) {
                        Logger.getLogger(p1.class.getName()).log(Level.SEVERE, null, ex);
                    }
                        // Start a timer for this archived message
                        // Start a timer for this archived message
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }        
        
        
    }//GEN-LAST:event_RestoreArchivedMessageButtonActionPerformed

    private void RemoteIPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RemoteIPActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_RemoteIPActionPerformed

    private void LocalIPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LocalIPActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_LocalIPActionPerformed

    private void deleteMessageFromMap(String message) {
    Iterator<Map.Entry<String, Long>> iterator = archivedMessagesMap.entrySet().iterator();
    while (iterator.hasNext()) {
        Map.Entry<String, Long> entry = iterator.next();
        String archivedMessage = entry.getKey();

        // Check if the archived message matches the provided message and the timestamp is within the deletion threshold
        if (archivedMessage.equals(message)) {
            // Remove the entry from the map
            iterator.remove();
            // Optionally, perform additional actions if needed
            System.out.println("Message deleted from the map: " + message);
        }
    }
}

    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea ArchivedMessagesTextArea;
    private javax.swing.JTextPane Chat;
    private javax.swing.JButton DeleteConvoButton;
    private javax.swing.JButton DeleteMessageButton;
    private javax.swing.JTextField LocalIP;
    private javax.swing.JTextField LocalPort;
    private javax.swing.JTextField RemoteIP;
    private javax.swing.JTextField RemotePort;
    private javax.swing.JButton RestoreArchivedMessageButton;
    private javax.swing.JButton Send;
    private javax.swing.JTextArea SendMessageTextField;
    private javax.swing.JButton StartListing;
    private javax.swing.JTextField Status;
    private javax.swing.JButton TestButton;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    // End of variables declaration//GEN-END:variables
}
