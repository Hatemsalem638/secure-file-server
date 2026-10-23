/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class MetaData {
    private final SimpleStringProperty name;
    private final SimpleIntegerProperty size;
    private final SimpleStringProperty owner;
    private final SimpleStringProperty modification;
    private final SimpleStringProperty creation;

    public MetaData(String name, int size, String owner, String modification, String creation) {
        this.name = new SimpleStringProperty(name);
        this.size = new SimpleIntegerProperty(size);
        this.owner = new SimpleStringProperty(owner);
        this.modification = new SimpleStringProperty(modification);
        this.creation = new SimpleStringProperty(creation);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public int getSize() {
        return size.get();
    }

    public void setSize(int size) {
        this.size.set(size);
    }

    public String getOwner() {
        return owner.get();
    }

    public void setOwner(String owner) {
        this.owner.set(owner);
    }

    public String getModification() {
        return modification.get();
    }

    public void setModification(String modification) {
        this.modification.set(modification);
    }

    public String getCreation() {
        return creation.get();
    }

    public void setCreation(String creation) {
        this.creation.set(creation);
    }
}

