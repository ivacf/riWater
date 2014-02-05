package uk.co.ribot.riwater;

/**
 * 
 * Interface used to return the result of an asynctask upon completion from an HTTP request.
 * 
 * @author Joe Birch
 *
 * @param <T>
 */
public interface AsyncTaskCompleteListener<T> {
    public void onTaskComplete(T result);
    public void onTaskFailed();
}