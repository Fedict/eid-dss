package be.fedict.eid.dss.protocol.simple.client;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * Service locator. Can handle both JNDI references as classname references.
 * Classname referencing can be useful in environments where you don't have a
 * full-blown Java EE application container available.
 * 
 * @param <T>
 *            the service type.
 * @author Frank Cornelis
 */
public class ServiceLocator<T> {

	private final String jndiLocation;

	private final String className;

	/**
	 * Main Constructor
	 * 
	 * @param initParam
	 *            servlet initialization parameter
	 * @param config
	 *            ServletConfig
	 * @throws javax.servlet.ServletException
	 *             something went wrong fetching the init parameter
	 */
	public ServiceLocator(String initParam, ServletConfig config)
			throws ServletException {

		this.jndiLocation = config.getInitParameter(initParam);
		this.className = config.getInitParameter(initParam + "Class");
	}

	/**
	 * @return if JNDI or ClassName is specified or not.
	 */
	public boolean isConfigured() {

		return null != this.jndiLocation || null != this.className;
	}

	/**
	 * Locates the service. Can return <code>null</code> in case the
	 * corresponding <code>init-param</code> was not set.
	 * 
	 * @return the service <T> or <code>null</code> if not set.
	 * @throws javax.servlet.ServletException
	 *             something went wrong trying to resolve the service
	 */
	@SuppressWarnings("unchecked")
	public T locateService() throws ServletException {
		try {
			T service;
			if (null != this.jndiLocation) {
				InitialContext initialContext = new InitialContext();
				service = (T) initialContext.lookup(this.jndiLocation);
			} else if (null != this.className) {
				Thread currentThread = Thread.currentThread();
				ClassLoader classLoader = currentThread.getContextClassLoader();
				Class<T> serviceClass = (Class<T>) classLoader
						.loadClass(this.className);
				service = serviceClass.newInstance();
			} else {
				service = null;
			}
			return service;
		} catch (NamingException e) {
			throw new ServletException("JNDI error: " + e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			throw new ServletException("Class not found: " + this.className, e);
		} catch (Exception e) {
			throw new ServletException("error: " + e.getMessage(), e);
		}
	}
}
