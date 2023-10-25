package formflow.library.data.builders;

import formflow.library.data.UserFile;

public class UserFileBuilder {

    public String docTypeLabel;

    public UserFileBuilder(String originalName){
        // required parameters
    }

    public UserFileBuilder setDocTypeLabel(String docTypeLabel){
        this.docTypeLabel = docTypeLabel;
        return this;
    }

    public UserFile build() {
        return new UserFile(this);
    }

    // optional parameters


}
