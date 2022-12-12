package fr.solunea.thaleia.webapp.pages.demo;

import java.io.Serializable;

public class AccountDetails implements Serializable {
    String email = "";
    String name = "";
    String phone = "";
    String company = "";

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    String password = "";
    boolean internalSigning;

    public boolean isInternalSigning() {
        return internalSigning;
    }

    public void setInternalSigning(boolean internalSigning) {
        this.internalSigning = internalSigning;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String toJson() {
        StringBuilder result= new StringBuilder();
        result.append("{");
        result.append("   \"name\":\""+getName()+"\",");
        result.append("   \"password\":\""+getPassword()+"\",");
        result.append("   \"phone\":\""+getPhone()+"\",");
        result.append("   \"company\":\""+getCompany()+"\"");
        result.append("}");
       return result.toString();
    }
}
