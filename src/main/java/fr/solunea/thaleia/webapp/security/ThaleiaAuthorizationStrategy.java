package fr.solunea.thaleia.webapp.security;

import org.apache.wicket.Component;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.authroles.authorization.strategies.role.IRoleCheckingStrategy;
import org.apache.wicket.authroles.authorization.strategies.role.RoleAuthorizationStrategy;
import org.apache.wicket.request.component.IRequestableComponent;

public class ThaleiaAuthorizationStrategy extends RoleAuthorizationStrategy {

    // private static Logger logger = Logger
    // .getLogger(ThaleiaAuthorizationStrategy.class);

    public ThaleiaAuthorizationStrategy(IRoleCheckingStrategy roleCheckingStrategy) {
        super(roleCheckingStrategy);
        add(new MyThaleiaAuthorizationStrategy());
    }

    public class MyThaleiaAuthorizationStrategy implements IAuthorizationStrategy {

        @Override
        public <T extends IRequestableComponent> boolean isInstantiationAuthorized(Class<T> componentClass) {
            // logger.debug("Autorisation d'instanciation de "
            // + componentClass.getName() + " ? );
            // On autorise tout
            return true;
        }

        @Override
        public boolean isActionAuthorized(Component component, Action action) {
            // On ne gère pas les actions
            return true;
        }

    }

}