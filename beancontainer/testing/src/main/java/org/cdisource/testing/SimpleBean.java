package org.cdisource.testing;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Simple named bean for testing
 * 
 * @author Andy Gibson
 *
 */
@Named("simpleBean")
public class SimpleBean {

	@Inject
	private InjectedBean injectedBean;
	
	@Inject @SomeOtherQualifier
	private InjectedBean otherInjectedBean;
	

	
	@Inject
	private SingletonBean singletonBean;
	
	
	public InjectedBean getInjectedBean() {
		return injectedBean;
	}

    public InjectedBean getOtherInjectedBean() {
        return otherInjectedBean;
    }
    
	public SingletonBean getSingletonBean() {
		return singletonBean;
	}
	
}
