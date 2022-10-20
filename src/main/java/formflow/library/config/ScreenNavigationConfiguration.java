package formflow.library.config;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

import formflow.library.config.submission.Action;
import lombok.Data;

import javax.naming.ConfigurationException;

/**
 * Screen navigation configuration class used to store navigation information about a
 * specific screen.
 */
@Data
public class ScreenNavigationConfiguration {
    private List<NextScreen> nextScreens = Collections.emptyList();
    private String subflow;
    //TODO: Implement callback
    private String callback;
    private String beforeSave;
    private Action beforeSaveAction;

    public void setBeforeSave(String beforeSave) throws ConfigurationException {
        try {
            this.beforeSave = beforeSave;
            System.out.println("beforeSave is set to: " + beforeSave);
            Class<?> clazz = Class.forName(beforeSave);
            Constructor<?> ctor = clazz.getConstructor();
            beforeSaveAction = (Action) ctor.newInstance();
        } catch (Exception e) {
            throw new ConfigurationException(String.format("Unable to setup action %s", beforeSave, e.getMessage()));
        }
    }
}
