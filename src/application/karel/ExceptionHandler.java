package application.karel;

public interface ExceptionHandler {
    public void karelException( int ID, KarelException e );
    public void stackOverflow( int ID, StackOverflowError e );
    public void notCompiledException( int ID, NullPointerException e );
    public void initializationException( int ID, NullPointerException e );
    public void doneExecuting( int ID );
    public void generalException( int ID, Exception e );

    public void compilationException( KarelException e );
}
