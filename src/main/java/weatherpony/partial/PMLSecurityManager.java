package weatherpony.partial;

public class PMLSecurityManager extends SecurityManager{
	private static final PMLSecurityManager accessibleManager = new PMLSecurityManager();

	@Override
    protected Class<?>[] getClassContext(){
        return super.getClassContext();
    }
    public static Class<?> getStackClass(int pos){
    	return accessibleManager.getClassContext()[pos+2];
    }
}
