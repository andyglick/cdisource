package org.cdisource.springintegration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

import org.cdisource.springintegration.springsupport.ApplicationContextLocator;
import org.cdisource.springintegration.springsupport.ApplicationContextLocatorManager;
import org.springframework.context.ApplicationContext;


public class SpringIntegrationExtention implements Extension {
	Set <SpringBean> springBeans = new HashSet<SpringBean>();
	
	
	public void processInjectionTarget (@Observes ProcessInjectionTarget<?> pit, BeanManager bm) {
		
		Set<InjectionPoint> injectionPoints = pit.getInjectionTarget().getInjectionPoints();
		
		synchronized (springBeans) {
			for (InjectionPoint point: injectionPoints){				
				Spring spring = point.getAnnotated().getAnnotation(Spring.class);
				if (spring!=null) {
					SpringBean springBean = new SpringBean(spring, (Class<?>) point.getType(), bm);
					springBeans.add(springBean);
				} else {
					SpringLookup springLookup = point.getAnnotated().getAnnotation(SpringLookup.class);
					if (springLookup!=null) {
						SpringBean springBean = new SpringBean(springLookup, (Class<?>) point.getType(), bm);
						springBeans.add(springBean);
					}
				}
			}

		}
		
	}
	
	
	void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
	}
	
	
	void afterBeanDiscovery( @Observes AfterBeanDiscovery abd, BeanManager bm) {
		synchronized (springBeans) {
			for (SpringBean bean : springBeans) {
				abd.addBean(bean);
			}	
		}
	}

	
	
	class SpringBean implements Bean <Object> {
		InjectionTarget<Object> it;
		Spring spring;
		SpringLookup lookup;
		Class<?> injectionType; 
		BeanManager bm;
	
		@SuppressWarnings("serial")
		class NamedLiteral extends AnnotationLiteral<Named> implements Named {

			@Override
			public String value() {
				return (spring!=null) ? spring.name() : lookup.value();
			}
			
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		SpringBean(Spring spring, Class<?> injectionType, BeanManager bm){
			this.spring = spring;
			this.injectionType = injectionType;
			this.bm = bm;
			AnnotatedType at = bm.createAnnotatedType(injectionType);
			it = bm.createInjectionTarget(at);
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public SpringBean(SpringLookup springLookup, Class<?> type,
				BeanManager bm) {
			this.lookup = springLookup;
			this.injectionType = type;
			this.bm = bm;
			AnnotatedType at = bm.createAnnotatedType(injectionType);
			it = bm.createInjectionTarget(at);

		}


		@Override
		public Class<?> getBeanClass() {
			return this.injectionType;
		}

		@Override
		public Set<InjectionPoint> getInjectionPoints() {
			return it.getInjectionPoints();
		}
		
		@Override
		public String getName() {
			//return spring!=null ? spring.name() : lookup.value();//causes Candi to fail for duplicate names
			return spring!=null ? spring.name() : null; 
		}

		@Override
		public Set<Annotation> getQualifiers() {
			Set<Annotation> qualifiers = new HashSet<Annotation>();	
			if (lookup==null) {
				qualifiers.add(new NamedLiteral());
			}
			if (spring!=null) {
				qualifiers.add( spring );
			} else {
				qualifiers.add( lookup );				
			}
			return qualifiers;
		}
		
		@Override
		public Class<? extends Annotation> getScope() {
			return Dependent.class;
		}

		@Override
		public Set<Class<? extends Annotation>> getStereotypes() {
			return Collections.emptySet();
		}

		@Override
		public Set<Type> getTypes() {
			Set<Type> types = new HashSet<Type>(); 
			types.add(this.injectionType); 
			types.add(Object.class); 
			return types;			
		}

		@Override
		public boolean isAlternative() {
			return false;
		}

		@Override
		public boolean isNullable() {
			return spring != null ? !spring.required() : false;
		}
		@Override
		public Object create(CreationalContext<Object> ctx) {
			ApplicationContext applicationContext = ApplicationContextLocatorManager.getInstance().locateApplicationContext();
			Object instance = null;
			if (spring!=null) {
			if (!spring.name().trim().equals("")) {
				if(!spring.required()) {
					if (applicationContext.containsBean(spring.name())) {
						instance = applicationContext.getBean(spring.name(), spring.type());						
					}
				} else {
					instance = applicationContext.getBean(spring.name(), spring.type());
				}
			} else {
				instance = applicationContext.getBean(spring.type());
			}
			} else {
				instance = applicationContext.getBean(lookup.value());				
			}
			it.inject(instance, ctx); 
			it.postConstruct(instance); 
			return instance;
		}

		@Override
		public void destroy(Object instance, CreationalContext<Object> ctx) {
			it.preDestroy(instance);
			it.dispose(instance); 
			ctx.release();
		}

		
	}


}
