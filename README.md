## Example usage

Consider an `Activity` you would like to start, `ExampleActivity`:
```java
public class ExampleActivity extends AppCompatActivity {
```

`ExampleActivity` may have a few parameters it receives from another `Activity`:
```java
private int numberToDisplay
private String stringToDisplay
private DataModel dataModel; // Parcelable or Serializable
```

With Piri, you can generate an Intent builder for `ExampleActivity`, and it only requires a few annotations.
```java
@PiriActivity
public class ExampleActivity extendsAppCompatActivity {
    private static String NUMBER_KEY = "extraNumber";
    private static String STRING_KEY = "extraString";
    private static String DATA_MODEL_KEY = "extraDataModel";
    
    @PiriParam(key = NUMBER_KEY)
    private int numberToDisplay;
    
    @PiriParam(key = STRING_KEY)
    private String stringToDisplay;
    
    @PiriParam(key = DATA_MODEL_KEY)
    private DataModel dataModel;
    
    ...
```

Okay cool! But how do we use it?
When you build your project, a class called `ExampleActivityIntentCreator` will be generated:
```java
Intent intent = new ExampleActivityIntentCreator(context)
        .numberToDisplay(number)
        .stringToDisplay(string)
        .dataModel(dataModel)
        .create();
startActivity(intent);
```

Easy! You can then retrieve your data when `ExampleActivity` is started:
```java
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_example);

    final Bundle bundle = getIntent().getExtras();
    if (bundle != null) {
        id = bundle.getLong(NUMBER_KEY);
        string = bundle.getString(STRING_KEY);
        dataModel = (DataModel) bundle.getSerializable(DATA_MODEL_KEY);
    }
    
    ...
```

## Where Piri comes from?
https://en.wikipedia.org/wiki/P%C3%AEr%C3%AE_Reis
