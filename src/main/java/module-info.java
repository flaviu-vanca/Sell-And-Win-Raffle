module raffle.sellandwinraffle {
   requires javafx.controls;
   requires javafx.fxml;

   opens fxml_files to javafx.fxml;
   exports raffle.controllers;
   opens raffle.controllers to javafx.fxml;
   exports raffle.main;
   opens raffle.main to javafx.fxml;
   opens raffle.models to javafx.base;
   exports raffle.models;
   opens stylesheets to javafx.fxml;
   opens i18n;
}
