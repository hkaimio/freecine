/*
 * MainWindow.java
 *
 * Created on 21. maaliskuuta 2008, 11:11
 */

package fi.kaimio.moviescan.ui;

import fi.kaimio.moviescan.FrameIterator;
import fi.kaimio.moviescan.Project;
import java.awt.image.RenderedImage;
import java.io.File;
import javax.swing.JOptionPane;

/**
 
 @author  harri
 */
public class MainWindow extends javax.swing.JFrame {
    
    Project prj;
    
    FrameIterator projectIter;
    
    RenderedImage currentImage = null;
    
    /** Creates new form MainWindow */
    public MainWindow() {
        initComponents();
        prj = Project.getProject( new File( "/home/harri/s8/tuhkimo" ) );
        projectIter = (FrameIterator) prj.iterator();
    }
    
    /** This method is called from within the constructor to
     initialize the form.
     WARNING: Do NOT modify this code. The content of this method is
     always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        frameNumField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        goToFrameBtn = new javax.swing.JButton();
        nextFrameBtn = new javax.swing.JButton();
        framePane = new FrameView();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Frame");

        goToFrameBtn.setText("Go");
        goToFrameBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goToFrameBtnActionPerformed(evt);
            }
        });

        nextFrameBtn.setText("Next");
        nextFrameBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextFrameBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout framePaneLayout = new javax.swing.GroupLayout(framePane);
        framePane.setLayout(framePaneLayout);
        framePaneLayout.setHorizontalGroup(
            framePaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 469, Short.MAX_VALUE)
        );
        framePaneLayout.setVerticalGroup(
            framePaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 337, Short.MAX_VALUE)
        );

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(framePane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(frameNumField, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(goToFrameBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nextFrameBtn)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(framePane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(goToFrameBtn)
                    .addComponent(nextFrameBtn)
                    .addComponent(frameNumField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void nextFrameBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextFrameBtnActionPerformed
        if ( projectIter.hasNext() ) {
            currentImage = projectIter.next();
            frameNumField.setText( Integer.toString( projectIter.getCurrentFrameNum() ));
            ((FrameView)framePane).setImage( currentImage );
        }
}//GEN-LAST:event_nextFrameBtnActionPerformed

    private void goToFrameBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_goToFrameBtnActionPerformed
        try {
            int frame = Integer.parseInt( frameNumField.getText() );
            projectIter.setNextFrameNum( frame );
            nextFrameBtnActionPerformed( evt );
        } catch ( NumberFormatException e ) {
            JOptionPane.showMessageDialog( this,
                    "Frame number must be number", "Incorrect frame number",
                    JOptionPane.ERROR_MESSAGE );
        }
    }//GEN-LAST:event_goToFrameBtnActionPerformed
    
    /**
     @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainWindow().setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField frameNumField;
    private javax.swing.JPanel framePane;
    private javax.swing.JButton goToFrameBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JButton nextFrameBtn;
    // End of variables declaration//GEN-END:variables
    
}