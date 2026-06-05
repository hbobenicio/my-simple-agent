package mysimpleagent.tools;

public interface Tool {
    String getName();
    String call(String args) throws Exception;
}
