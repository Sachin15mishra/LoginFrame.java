package VehicleIdentificationSystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * VehicleSystemPortal - single-file Swing application
 * - Sidebar navigation, central cards, right RC preview
 * - Registration generates reg number only after full details provided
 * - Insurance and PUC modules working by registration number lookup
 * - Vehicles persisted to vehicles.dat
 */
public class VehicleSystemPortal extends JFrame {
    // ---------------- Model ----------------
    static class Vehicle implements Serializable {
        private static final long serialVersionUID = 2L;
        String type, brand, model, regNumber, owner, state, district, districtCode;
        LocalDate registrationDate;
        // Insurance
        boolean hasInsurance = false; String insuranceProvider = ""; LocalDate insuranceExpiry = null;
        // PUC
        boolean hasPUC = false; LocalDate pucExpiry = null;
        // Challan
        double challanAmount = 0.0;

        public Vehicle(String type, String brand, String model, String regNumber, String owner,
                       String state, String district, String districtCode) {
            this.type = type; this.brand = brand; this.model = model; this.regNumber = regNumber; this.owner = owner;
            this.state = state; this.district = district; this.districtCode = districtCode;
            this.registrationDate = LocalDate.now();
        }

        public String getRegNumber() {
            return regNumber;
        }
        public String owner() {
            return owner;
        }

        public void issueInsurance(String provider, LocalDate expiry) {
            this.hasInsurance = true;
            this.insuranceProvider = provider;
            this.insuranceExpiry = expiry;
        }
        public void issuePUC(LocalDate expiry) {
            this.hasPUC = true;
            this.pucExpiry = expiry;
        }
        public void addChallan(double amt) {
            this.challanAmount += amt;
        }
        public void payChallan(double amt) {
            this.challanAmount -= amt;
            if (this.challanAmount < 0)
                this.challanAmount = 0;
        }

        public String detailedInfo() {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
            StringBuilder sb = new StringBuilder();
            sb.append("Registration: ").append(regNumber).append("\n");
            sb.append("Owner: ").append(owner).append("\n");
            sb.append("Type: ").append(type).append("\n");
            sb.append("Brand: ").append(brand).append("\n");
            sb.append("Model: ").append(model).append("\n");
            sb.append("State: ").append(state).append("\n");
            sb.append("District: ").append(district).append(" ("+districtCode+")\n");
            sb.append("Registered On: ").append(registrationDate.format(f)).append("\n");
            sb.append("Insurance: ").append(hasInsurance ? (insuranceProvider+" until "+insuranceExpiry.format(f)) : "None").append("\n");
            sb.append("PUC: ").append(hasPUC ? (pucExpiry.format(f)) : "None").append("\n");
            sb.append("Challan Due: ₹").append(String.format("%.2f", challanAmount)).append("\n");
            return sb.toString();
        }
    }

    // ---------------- Storage ----------------
    static class Storage {
        private static final String FILE = "vehicles.dat";
        @SuppressWarnings("unchecked")
        public static ArrayList<Vehicle> load() {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE))) {
                return (ArrayList<Vehicle>) ois.readObject();
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        public static void save(ArrayList<Vehicle> list) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE))) { oos.writeObject(list); }
            catch (IOException e) { e.printStackTrace(); }
        }
    }

    // ---------------- App state ----------------
    private final ArrayList<Vehicle> vehicles = Storage.load();
    private final Map<String,Integer> seriesTracker = new HashMap<>();
    private final Map<String,Integer> numberTracker = new HashMap<>();

    // UI
    private final CardLayout centerCards = new CardLayout();
    private final JPanel pnlCenter = new JPanel(centerCards);
    private final JPanel pnlRight = new JPanel(new BorderLayout());

    // Register form fields
    private final JTextField tfOwner = new JTextField();
    private final JComboBox<String> cbVehicleType = new JComboBox<>(new String[]{"Car","Motorcycle","Truck","Bus","Other"});
    private final JTextField tfBrand = new JTextField();
    private final JTextField tfModel = new JTextField();
    private final JComboBox<String> cbState = new JComboBox<>();
    private final JTextField tfDistrict = new JTextField();
    private final JTextField tfDistrictCode = new JTextField();
    private final JLabel lblGeneratedReg = new JLabel("—", SwingConstants.CENTER);

    // Feature lookup
    private final JTextField tfLookupReg = new JTextField(20);

    // Right summary
    private final JTextArea taSummary = new JTextArea();

    // States list
    private static final String[] STATES = {
            "Andhra Pradesh","Arunachal Pradesh","Assam","Bihar","Chhattisgarh","Goa","Gujarat","Haryana","Himachal Pradesh","Jharkhand",
            "Karnataka","Kerala","Madhya Pradesh","Maharashtra","Manipur","Meghalaya","Mizoram","Nagaland","Odisha","Punjab","Rajasthan",
            "Sikkim","Tamil Nadu","Telangana","Tripura","Uttar Pradesh","Uttarakhand","West Bengal",
            "Andaman and Nicobar Islands","Chandigarh","Dadra and Nagar Haveli and Daman and Diu","Delhi","Jammu & Kashmir","Ladakh","Lakshadweep","Puducherry"
    };

    private final Font headerFont = new Font("SansSerif", Font.BOLD, 20);
    private final Font normalFont = new Font("SansSerif", Font.PLAIN, 14);
    private final Color brandColor = new Color(14,76,161);

    private final String currentUser;
    public VehicleSystemPortal(String username) {
        this.currentUser = username;
        setTitle("Vehicle Portal");
        setSize(1150,720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(topHeader(), BorderLayout.NORTH);
        add(leftMenu(), BorderLayout.WEST);

        pnlCenter.add(registerPanel(), "REGISTER");
        pnlCenter.add(featuresPanel(), "FEATURES");
        pnlCenter.add(insurancePanel(), "INSURANCE");
        pnlCenter.add(pucPanel(), "PUC");
        pnlCenter.add(viewAllPanel(), "VIEWALL");
        add(pnlCenter, BorderLayout.CENTER);

        pnlRight.setPreferredSize(new Dimension(340,0));
        pnlRight.setBorder(new EmptyBorder(18,18,18,18));
        add(pnlRight, BorderLayout.EAST);
        refreshSummary(null);

        centerCards.show(pnlCenter, "REGISTER");
        setVisible(true);
    }

    // ---------- UI builders ----------
    private JPanel topHeader() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(brandColor);
        JLabel title = new JLabel("VEHICLE PORTAL", SwingConstants.LEFT); title.setForeground(Color.WHITE); title.setFont(headerFont); title.setBorder(new EmptyBorder(12,16,12,12)); p.add(title, BorderLayout.WEST);
        JLabel sub = new JLabel("Register vehicles — manage Insurance / PUC / Challan", SwingConstants.RIGHT); sub.setForeground(Color.WHITE); sub.setBorder(new EmptyBorder(8,8,8,16)); p.add(sub, BorderLayout.EAST);
        return p;
    }

    private JPanel leftMenu() {
        JPanel v = new JPanel(); v.setPreferredSize(new Dimension(220,0)); v.setLayout(new BoxLayout(v, BoxLayout.Y_AXIS)); v.setBorder(new EmptyBorder(16,12,16,12)); v.setBackground(new Color(250,250,250));

        // Add user info
        if (currentUser != null) {
            JLabel userLabel = new JLabel("👤 " + currentUser, SwingConstants.CENTER);
            userLabel.setForeground(Color.BLUE);
            userLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            v.add(userLabel);

            v.add(Box.createRigidArea(new Dimension(0, 15))); // spacing
        }

        JLabel menuTitle = new JLabel("Menu"); menuTitle.setFont(new Font("SansSerif", Font.BOLD, 18)); v.add(menuTitle); v.add(Box.createVerticalStrut(8));
        v.add(navButton("Register Vehicle", e -> centerCards.show(pnlCenter, "REGISTER"))); v.add(Box.createVerticalStrut(8));
        v.add(navButton("Search / Features", e -> centerCards.show(pnlCenter, "FEATURES"))); v.add(Box.createVerticalStrut(8));
        v.add(navButton("Insurance", e -> centerCards.show(pnlCenter, "INSURANCE"))); v.add(Box.createVerticalStrut(8));
        v.add(navButton("Pollution (PUC)", e -> centerCards.show(pnlCenter, "PUC"))); v.add(Box.createVerticalStrut(8));
        v.add(navButton("View All Registered", e -> { centerCards.show(pnlCenter, "VIEWALL"); loadAllToSummary(); })); v.add(Box.createVerticalStrut(8));
        v.add(navButton("Export CSV", e -> exportCSV())); v.add(Box.createVerticalGlue()); v.add(navButton("Exit", e -> { Storage.save(vehicles); System.exit(0); }));
        return v;
    }

    private JButton navButton(String text, ActionListener al) {
        JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        b.setFont(normalFont);
        b.setBackground(new Color(230,240,255));
        b.setFocusPainted(false);
        b.addActionListener(al);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel registerPanel() {
        JPanel outer = new JPanel(new BorderLayout()); outer.setBorder(new EmptyBorder(18,18,18,18));
        JLabel h = new JLabel("Register New Vehicle"); h.setFont(new Font("SansSerif", Font.BOLD, 18));
        JPanel top = new JPanel(new BorderLayout()); top.add(h, BorderLayout.WEST); top.add(lblGeneratedReg, BorderLayout.EAST); lblGeneratedReg.setFont(new Font("Monospaced", Font.BOLD, 16)); lblGeneratedReg.setForeground(new Color(40,40,40)); top.setBorder(new EmptyBorder(6,6,12,6)); outer.add(top, BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout()); form.setBorder(new EmptyBorder(8,8,8,8)); GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(8,8,8,8); g.fill = GridBagConstraints.HORIZONTAL; int row = 0;
        g.gridx=0; g.gridy=row; g.weightx=0.25; form.add(new JLabel("Owner Name:"), g); g.gridx=1; g.gridy=row++; g.weightx=0.75; form.add(tfOwner, g);
        g.gridx=0; g.gridy=row; g.weightx=0.25; form.add(new JLabel("Vehicle Type:"), g); g.gridx=1; g.gridy=row++; g.weightx=0.75; form.add(cbVehicleType, g);
        g.gridx=0; g.gridy=row; form.add(new JLabel("Brand:"), g); g.gridx=1; g.gridy=row++; form.add(tfBrand, g);
        g.gridx=0; g.gridy=row; form.add(new JLabel("Model:"), g); g.gridx=1; g.gridy=row++; form.add(tfModel, g);
        g.gridx=0; g.gridy=row; form.add(new JLabel("State:"), g); g.gridx=1; g.gridy=row++; form.add(cbState, g);
        for (String s : STATES) cbState.addItem(s);
        g.gridx=0; g.gridy=row; form.add(new JLabel("District (enter):"), g); g.gridx=1; g.gridy=row++; form.add(tfDistrict, g);
        g.gridx=0; g.gridy=row; form.add(new JLabel("District Code (e.g. 01):"), g); g.gridx=1; g.gridy=row++; form.add(tfDistrictCode, g);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); JButton btnGen = new JButton("Preview Reg No"); JButton btnRegister = new JButton("Register Vehicle");
        btnGen.addActionListener(e -> { String st = (String)cbState.getSelectedItem(); String di = tfDistrict.getText().trim(); if (st==null || di.isEmpty()) { JOptionPane.showMessageDialog(this, "Select state and enter district to preview"); return; } String reg = generateRegNumber(st, di, tfDistrictCode.getText().trim()); lblGeneratedReg.setText(reg); refreshSummary(null); });
        btnRegister.addActionListener(e -> { String owner = tfOwner.getText().trim(); String type = (String) cbVehicleType.getSelectedItem(); String brand = tfBrand.getText().trim(); String model = tfModel.getText().trim(); String state = (String)cbState.getSelectedItem(); String district = tfDistrict.getText().trim(); String dcode = tfDistrictCode.getText().trim(); String reg = lblGeneratedReg.getText(); if (owner.isEmpty() || type==null || state==null || district.isEmpty() || dcode.isEmpty()) { JOptionPane.showMessageDialog(this, "Please fill mandatory fields and preview registration number before registering."); return; } if (reg==null || reg.equals("—")) { reg = generateRegNumber(state, district, dcode); } Vehicle v = new Vehicle(type, brand, model, reg, owner, state, district, dcode); vehicles.add(v); Storage.save(vehicles); JOptionPane.showMessageDialog(this, "Registered successfully!\nRegistration No: " + reg); clearRegistrationForm(); refreshSummary(v); });
        btns.add(btnGen); btns.add(btnRegister); g.gridx=1; g.gridy=row++; form.add(btns, g);
        outer.add(form, BorderLayout.CENTER); refreshRightPlaceholder(); return outer;
    }

    private JPanel featuresPanel() {
        JPanel outer = new JPanel(new BorderLayout()); outer.setBorder(new EmptyBorder(18,18,18,18)); JLabel h = new JLabel("Lookup by Registration Number — Actions"); h.setFont(new Font("SansSerif", Font.BOLD, 16)); outer.add(h, BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout()); JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT,8,8)); tfLookupReg.setPreferredSize(new Dimension(420,30)); JButton btnFind = new JButton("Find"); JButton btnShowIns = new JButton("Show Insurance"); JButton btnShowPUC = new JButton("Show PUC"); JButton btnChallan = new JButton("Challan"); btnFind.addActionListener(e -> doFind()); btnShowIns.addActionListener(e -> showInsurance()); btnShowPUC.addActionListener(e -> showPUC()); btnChallan.addActionListener(e -> doChallanPayment()); search.add(new JLabel("Registration No:")); search.add(tfLookupReg); search.add(btnFind); search.add(btnShowIns); search.add(btnShowPUC); search.add(btnChallan); mid.add(search, BorderLayout.NORTH); JTextArea ta = new JTextArea(); ta.setEditable(false); JScrollPane jsp = new JScrollPane(ta); mid.add(jsp, BorderLayout.CENTER); outer.add(mid, BorderLayout.CENTER); return outer; }

    private JPanel insurancePanel() { JPanel p = new JPanel(new GridBagLayout()); p.setBorder(new EmptyBorder(18,18,18,18)); GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(8,8,8,8); c.fill = GridBagConstraints.HORIZONTAL; JTextField tfReg = new JTextField(18); JTextField tfProvider = new JTextField(16); JTextField tfExpiry = new JTextField(12); c.gridx=0; c.gridy=0; p.add(new JLabel("Registration No:"), c); c.gridx=1; p.add(tfReg, c); c.gridx=0; c.gridy=1; p.add(new JLabel("Insurance Provider:"), c); c.gridx=1; p.add(tfProvider, c); c.gridx=0; c.gridy=2; p.add(new JLabel("Expiry (YYYY-MM-DD):"), c); c.gridx=1; p.add(tfExpiry, c); JButton btnIssue = new JButton("Issue Insurance"); JLabel msg = new JLabel(" "); msg.setForeground(new Color(10,90,10)); c.gridx=1; c.gridy=3; p.add(btnIssue, c); c.gridx=1; c.gridy=4; p.add(msg, c); btnIssue.addActionListener(e -> { String reg = tfReg.getText().trim(); if (reg.isEmpty()) { msg.setText("Enter registration number"); return; } Vehicle v = findVehicleByReg(reg); if (v==null) { msg.setText("Vehicle not found"); return; } try { LocalDate d = LocalDate.parse(tfExpiry.getText().trim()); v.issueInsurance(tfProvider.getText().trim(), d); Storage.save(vehicles); msg.setText("Insurance issued"); refreshSummary(v); } catch (Exception ex) { msg.setText("Invalid date format"); } }); return p; }

    private JPanel pucPanel() { JPanel p = new JPanel(new GridBagLayout()); p.setBorder(new EmptyBorder(18,18,18,18)); GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(8,8,8,8); c.fill = GridBagConstraints.HORIZONTAL; JTextField tfReg = new JTextField(18); JTextField tfExpiry = new JTextField(12); JLabel msg = new JLabel(" "); msg.setForeground(new Color(10,90,10)); c.gridx=0; c.gridy=0; p.add(new JLabel("Registration No:"), c); c.gridx=1; p.add(tfReg, c); c.gridx=0; c.gridy=1; p.add(new JLabel("PUC Expiry (YYYY-MM-DD):"), c); c.gridx=1; p.add(tfExpiry, c); JButton btn = new JButton("Issue PUC"); c.gridx=1; c.gridy=2; p.add(btn, c); c.gridx=1; c.gridy=3; p.add(msg, c); btn.addActionListener(e -> { Vehicle v = findVehicleByReg(tfReg.getText().trim()); if (v==null) { msg.setText("Vehicle not found"); return; } try { LocalDate d = LocalDate.parse(tfExpiry.getText().trim()); v.issuePUC(d); Storage.save(vehicles); msg.setText("PUC issued"); refreshSummary(v); } catch(Exception ex) { msg.setText("Invalid date"); } }); return p; }

    private JPanel viewAllPanel() { JPanel p = new JPanel(new BorderLayout()); p.setBorder(new EmptyBorder(12,12,12,12)); JLabel h = new JLabel("All Registered Vehicles"); h.setFont(new Font("SansSerif", Font.BOLD, 16)); p.add(h, BorderLayout.NORTH); JTextArea ta = new JTextArea(); ta.setEditable(false); JScrollPane sp = new JScrollPane(ta); p.add(sp, BorderLayout.CENTER); StringBuilder sb = new StringBuilder(); for (Vehicle v : vehicles) sb.append(v.detailedInfo()).append("\n--------------------\n"); if (vehicles.isEmpty()) sb.append("No vehicles yet."); ta.setText(sb.toString()); return p; }

    // ---------- Helpers ----------
    private void refreshRightPlaceholder() { pnlRight.removeAll(); pnlRight.add(summaryCardPlaceholder(), BorderLayout.NORTH); pnlRight.revalidate(); pnlRight.repaint(); }
    private JPanel summaryCardPlaceholder() { JPanel card = new JPanel(new BorderLayout()); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200,200,200)), new EmptyBorder(10,10,10,10))); JLabel ttl = new JLabel("Registration Summary", SwingConstants.CENTER); ttl.setFont(new Font("SansSerif", Font.BOLD, 16)); card.add(ttl, BorderLayout.NORTH); JTextArea ta = new JTextArea(); ta.setEditable(false); ta.setFont(new Font("Monospaced", Font.PLAIN, 12)); ta.setText("Generate a registration number then register to see the RC preview here.\n\nAfter registration you can issue Insurance or PUC using the registration number."); card.add(new JScrollPane(ta), BorderLayout.CENTER); return card; }
    private void refreshSummary(Vehicle v) { pnlRight.removeAll(); JPanel card = new JPanel(new BorderLayout()); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220,220,220)), new EmptyBorder(12,12,12,12))); JLabel title = new JLabel("RC Preview", SwingConstants.CENTER); title.setFont(new Font("SansSerif", Font.BOLD, 15)); card.add(title, BorderLayout.NORTH); JTextArea ta = new JTextArea(); ta.setEditable(false); ta.setFont(new Font("Monospaced", Font.PLAIN, 12)); if (v==null) { String gen = lblGeneratedReg.getText(); if (gen==null || gen.equals("—")) ta.setText("No registration generated yet. Use Preview Reg No to see RC here."); else ta.setText("Reg No: "+gen+"\nOwner: "+tfOwner.getText()+"\nModel: "+tfModel.getText()); } else { ta.setText(v.detailedInfo()); } card.add(new JScrollPane(ta), BorderLayout.CENTER); JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER)); JButton btnPrint = new JButton("Print (disabled)"); btnPrint.setEnabled(false); footer.add(btnPrint); card.add(footer, BorderLayout.SOUTH); pnlRight.add(card, BorderLayout.NORTH); pnlRight.revalidate(); pnlRight.repaint(); }

    private void loadAllToSummary() { JPanel card = new JPanel(new BorderLayout()); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220,220,220)), new EmptyBorder(10,10,10,10))); JLabel t = new JLabel("All Vehicles (quick view)"); t.setFont(new Font("SansSerif", Font.BOLD, 14)); card.add(t, BorderLayout.NORTH); JTextArea ta = new JTextArea(); ta.setEditable(false); StringBuilder sb = new StringBuilder(); for (Vehicle v : vehicles) sb.append(v.getRegNumber()).append(" — ").append(v.owner()).append("\n"); if (vehicles.isEmpty()) sb.append("No vehicles registered yet."); ta.setText(sb.toString()); card.add(new JScrollPane(ta), BorderLayout.CENTER); pnlRight.removeAll(); pnlRight.add(card, BorderLayout.NORTH); pnlRight.revalidate(); pnlRight.repaint(); }

    private Vehicle findVehicleByReg(String reg) { if (reg==null) return null; for (Vehicle v : vehicles) if (v.getRegNumber().equalsIgnoreCase(reg.trim())) return v; return null; }
    private void doFind() { String r = tfLookupReg.getText().trim(); if (r.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter registration number"); return; } Vehicle v = findVehicleByReg(r); if (v==null) { JOptionPane.showMessageDialog(this, "Vehicle not found for: " + r); return; } refreshSummary(v); JOptionPane.showMessageDialog(this, v.detailedInfo()); }
    private void showInsurance() { String r = tfLookupReg.getText().trim(); if (r.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter registration number"); return; } Vehicle v = findVehicleByReg(r); if (v==null) { JOptionPane.showMessageDialog(this, "Vehicle not found"); return; } if (!v.hasInsurance) JOptionPane.showMessageDialog(this, "No insurance issued for " + r); else JOptionPane.showMessageDialog(this, "Insurance: " + v.insuranceProvider + " until " + v.insuranceExpiry); }
    private void showPUC() { String r = tfLookupReg.getText().trim(); if (r.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter registration number"); return; } Vehicle v = findVehicleByReg(r); if (v==null) { JOptionPane.showMessageDialog(this, "Vehicle not found"); return; } if (!v.hasPUC) JOptionPane.showMessageDialog(this, "No PUC issued for " + r); else JOptionPane.showMessageDialog(this, "PUC valid until " + v.pucExpiry); }
    private void doChallanPayment() { String r = tfLookupReg.getText().trim(); if (r.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter registration number to pay challan"); return; } Vehicle v = findVehicleByReg(r); if (v==null) { JOptionPane.showMessageDialog(this, "Vehicle not found for: " + r); return; } if (v.challanAmount == 0) { v.challanAmount = Math.round((500 + Math.random()*4500)); } String amt = JOptionPane.showInputDialog(this, "Challan due: ₹" + v.challanAmount + "\nEnter amount to pay:"); try { double pay = Double.parseDouble(amt); v.payChallan(pay); Storage.save(vehicles); JOptionPane.showMessageDialog(this, "Payment accepted. Remaining challan: ₹" + v.challanAmount); refreshSummary(v); } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid amount or cancelled"); } }

    private void clearRegistrationForm() { tfOwner.setText(""); tfBrand.setText(""); tfModel.setText(""); tfDistrict.setText(""); tfDistrictCode.setText(""); lblGeneratedReg.setText("—"); }

    // ---------- Reg number generation ----------
    private String generateRegNumber(String stateFull, String districtFull, String districtCode) {
        String stCode = extractStateCode(stateFull);
        String distCode = formatDistrictCode(districtCode);
        String key = stCode + "|" + distCode;
        int seriesIdx = seriesTracker.getOrDefault(key, 0);
        int num = numberTracker.getOrDefault(key, 0) + 1;
        if (num > 9999) { seriesIdx++; num = 1; }
        seriesTracker.put(key, seriesIdx); numberTracker.put(key, num);
        String series = seriesIndexToString(seriesIdx);
        String number = String.format("%04d", num);
        return stCode + " " + distCode + " " + series + " " + number;
    }

    private static final Map<String, String> STATE_CODES = new HashMap<>();
    static {
        STATE_CODES.put("Andhra Pradesh","AP");
        STATE_CODES.put("Arunachal Pradesh","AR");
        STATE_CODES.put("Assam","AS");
        STATE_CODES.put("Bihar","BR");
        STATE_CODES.put("Chhattisgarh","CG");
        STATE_CODES.put("Goa","GA");
        STATE_CODES.put("Gujarat","GJ");
        STATE_CODES.put("Haryana","HR");
        STATE_CODES.put("Himachal Pradesh","HP");
        STATE_CODES.put("Jharkhand","JH");
        STATE_CODES.put("Karnataka","KA");
        STATE_CODES.put("Kerala","KL");
        STATE_CODES.put("Madhya Pradesh","MP");
        STATE_CODES.put("Maharashtra","MH");
        STATE_CODES.put("Manipur","MN");
        STATE_CODES.put("Meghalaya","ML");
        STATE_CODES.put("Mizoram","MZ");
        STATE_CODES.put("Nagaland","NL");
        STATE_CODES.put("Odisha","OD");   // Was OR earlier, OD now used
        STATE_CODES.put("Punjab","PB");
        STATE_CODES.put("Rajasthan","RJ");
        STATE_CODES.put("Sikkim","SK");
        STATE_CODES.put("Tamil Nadu","TN");
        STATE_CODES.put("Telangana","TS");
        STATE_CODES.put("Tripura","TR");
        STATE_CODES.put("Uttar Pradesh","UP");
        STATE_CODES.put("Uttarakhand","UK"); // Earlier UA
        STATE_CODES.put("West Bengal","WB");
        STATE_CODES.put("Andaman and Nicobar Islands","AN");
        STATE_CODES.put("Chandigarh","CH");
        STATE_CODES.put("Dadra and Nagar Haveli and Daman and Diu","DD");
        STATE_CODES.put("Delhi","DL");
        STATE_CODES.put("Jammu & Kashmir","JK");
        STATE_CODES.put("Ladakh","LA");
        STATE_CODES.put("Lakshadweep","LD");
        STATE_CODES.put("Puducherry","PY");
    }


    private static String seriesIndexToString(int idx) {
        int high = idx / 26;
        int low = idx % 26;
        char a = (char) ('A' + Math.max(0, Math.min(25, high)));
        char b = (char) ('A' + Math.max(0, Math.min(25, low)));
        return "" + a + b;
    }
    private static String extractStateCode(String stateFull) {
        if (stateFull == null) return "XX";
        return STATE_CODES.getOrDefault(stateFull, "XX");
    }
    private static String formatDistrictCode(String d) {
        String t = d.trim();
        if (t.matches("\\d{2}")) return t;                  // 01, 32, 27
        if (t.matches("[A-Za-z]{2}")) return t.toUpperCase(); // AA, DL etc.
        throw new IllegalArgumentException("Invalid District Code");
    }
    private static String extractDistrictCodeFromName(String districtFull) { if (districtFull==null) return "00"; String cleaned = districtFull.replaceAll("[^A-Za-z]", ""); if (cleaned.length()>=2) return cleaned.substring(0,2).toUpperCase(); return "00"; }

    // ---------- CSV export ----------
    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save vehicles CSV");
        int sel = chooser.showSaveDialog(this);
        if (sel == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(f)) {
                pw.println("RegNumber,Owner,Type,Brand,Model,State,District,DistrictCode,RegisteredDate,Challan,HasInsurance,InsuranceExpiry,HasPUC,PUCExpiry");
                DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                for (Vehicle v : vehicles) {
                    pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%b,%s,%b,%s\n", v.regNumber, escapeCsv(v.owner), v.type, escapeCsv(v.brand), escapeCsv(v.model), escapeCsv(v.state), escapeCsv(v.district), escapeCsv(v.districtCode), v.registrationDate.format(df), v.challanAmount, v.hasInsurance, v.insuranceExpiry == null ? "" : v.insuranceExpiry.toString(), v.hasPUC, v.pucExpiry == null ? "" : v.pucExpiry.toString());
                }
                JOptionPane.showMessageDialog(this, "Exported to: " + f.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
            }
        }
    }

    private static String escapeCsv(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }

    // ---------- Main ----------
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(VehicleSystemPortal::new);
//    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

}
