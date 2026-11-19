package com.stockmanagement.ui.components;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Home page banner: live clock with "Good Morning / Afternoon / Evening" and today's date. */
public class GreetingClockPanel extends JPanel {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm:ss a");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");

    private final String userName;
    private final JLabel greetingLabel = new JLabel();
    private final JLabel dateLabel = new JLabel();
    private final JLabel timeLabel = new JLabel();

    public GreetingClockPanel(String userName) {
        super(new BorderLayout(10, 0));
        this.userName = userName;

        setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        greetingLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        timeLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(greetingLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(dateLabel);

        add(left, BorderLayout.CENTER);
        add(timeLabel, BorderLayout.EAST);

        tick();
        new Timer(1000, e -> tick()).start();
    }

    /** 05:00-11:59 morning, 12:00-16:59 afternoon, everything else evening. */
    static String greetingFor(int hour) {
        if (hour >= 5 && hour < 12) return "Good Morning";
        if (hour >= 12 && hour < 17) return "Good Afternoon";
        return "Good Evening";
    }

    private void tick() {
        LocalDateTime now = LocalDateTime.now();
        greetingLabel.setText(greetingFor(now.getHour()) + ", " + userName);
        dateLabel.setText(now.format(DATE));
        timeLabel.setText(now.format(TIME));
    }
}
