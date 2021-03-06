package com.paragonict.webapp.threader.services;

import java.io.IOException;
import java.util.Properties;

import org.apache.tapestry5.ComponentParameterConstants;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ScopeConstants;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.SubModule;
import org.apache.tapestry5.services.ComponentRequestFilter;
import org.apache.tapestry5.services.javascript.JavaScriptStack;
import org.chenillekit.tapestry.core.ChenilleKitCoreModule;

import com.paragonict.tapisser.SymbolConstants;
import com.paragonict.tapisser.services.TapisserModule;
import com.paragonict.webapp.threader.Constants;
import com.paragonict.webapp.threader.services.filter.RequiresLoginFilter;
import com.paragonict.webapp.threader.services.impl.AccountServiceImpl;
import com.paragonict.webapp.threader.services.impl.MailFetcherImpl;
import com.paragonict.webapp.threader.services.impl.MailServiceImpl;
import com.paragonict.webapp.threader.services.impl.MailSessionImpl;
import com.paragonict.webapp.threader.services.impl.MailStoreImpl;
import com.paragonict.webapp.threader.services.internal.WizardScriptStack;

/**
 * This module is automatically included as part of the Tapestry IoC Registry, it's a good place to
 * configure and extend Tapestry, or to place your own service definitions.
 */
@SubModule({TapisserModule.class,ChenilleKitCoreModule.class})
public class ThreaderModule
{
    public static void bind(ServiceBinder binder)
    {
    	binder.bind(IMailSession.class,MailSessionImpl.class); // singleton
        binder.bind(IMailService.class,MailServiceImpl.class); // singleton
        binder.bind(IMailFetcher.class,MailFetcherImpl.class); // singleton
        binder.bind(IMailStore.class,MailStoreImpl.class).scope(ScopeConstants.PERTHREAD);
        binder.bind(IAccountService.class,AccountServiceImpl.class).scope(ScopeConstants.PERTHREAD);
   }

    public static void contributeFactoryDefaults(
            MappedConfiguration<String, Object> configuration) {
        // The application version number is incorprated into URLs for some
        // assets. Web browsers will cache assets because of the far future expires
        // header. If existing assets are changed, the version number should also
        // change, to force the browser to download new versions. This overrides Tapesty's default
        // (a random hexadecimal number), but may be further overriden by DevelopmentModule or
        // QaModule.
        configuration.override(org.apache.tapestry5.SymbolConstants.APPLICATION_VERSION, "1.0-SNAPSHOT");
        configuration.override(SymbolConstants.BOOTSTRAP_ENABLED, true);
    }

    public static void contributeApplicationDefaults(
            MappedConfiguration<String, Object> configuration)
    {
        // Contributions to ApplicationDefaults will override any contributions to
        // FactoryDefaults (with the same key). Here we're restricting the supported
        // locales to just "en" (English). As you add localised message catalogs and other assets,
        // you can extend this list of locales (it's a comma separated series of locale names;
        // the first locale name is the default when there's no reasonable match).
        configuration.add(org.apache.tapestry5.SymbolConstants.SUPPORTED_LOCALES, "en");
        
		configuration.add(ComponentParameterConstants.GRID_TABLE_CSS_CLASS,"table");
		configuration.add(org.apache.tapestry5.SymbolConstants.HMAC_PASSPHRASE, Constants.HMAC_PASSPHRASE);
		// wtf... zo simple kan het zijn?
		configuration.add(org.apache.tapestry5.SymbolConstants.EXCEPTION_REPORT_PAGE, "Error");
		Properties props = new Properties();
		try {
			props.load(ThreaderModule.class.getResourceAsStream("/threader.properties"));
		} catch (IOException e) {
			System.err.println("Could not load configuration file ! ");
			e.printStackTrace();
		}
		configuration.add(Constants.SYMBOL_MAIL_DEBUG,props.getProperty(Constants.SYMBOL_MAIL_DEBUG, "false"));
    }
    
    public static void contributeComponentRequestHandler(OrderedConfiguration<ComponentRequestFilter> configuration) {
        configuration.addInstance("RequiresLogin", RequiresLoginFilter.class,"after:ErrorFilter");
    }
    
    public static void contributeJavaScriptStackSource(MappedConfiguration<String, JavaScriptStack> configuration) {
    	configuration.addInstance("Wizard", WizardScriptStack.class);
    }
    
    /*
    public static void contributeTypeCoercer(Configuration<CoercionTuple> configuration) {
    	// Address[] to String
        Coercion<Address[], String> adrToString = new Coercion<Address[], String>() {
	
        	public String coerce(Address[] input) {
        		String address = "";
        		for (Address a: input) {
            	   address = address + a.toString();
        		}
        		return address;
            }
        };
      
        configuration.add(new CoercionTuple<Address[], String>(Address[].class, String.class, adrToString));
        
     // String to Address[]
        Coercion<String, Address[]> strToAdr = new Coercion<String, Address[]>() {
	
        	public Address[] coerce(String input) {
        		Address[] addresses = new Address[] {};
        		for (String s: input.split(",")) {
        			Address a;
					try {
						a = new InternetAddress(s);
						addresses = (Address[]) ArrayUtils.add(addresses, a);
					} catch (AddressException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        			
        		}
        		return addresses;
            }
        };
      
        configuration.add(new CoercionTuple<String,Address[]>(String.class,Address[].class, strToAdr));
        System.err.println("Coercer config: "+ configuration);
    }*/
    
 
}
