## Example usage

Consider an `Activity` you would like to start, `ExampleActivity`:
```
public class ExampleActivity extends AppCompatActivity {
```

`ExampleActivity` may have a few parameters it receives from another `Activity`:
```
private int numberToDisplay
private String stringToDisplay
private DataModel dataModel; // parcelable or serializable
```

With Piri, you can generate an Intent builder for `ExampleActivity`, and it only requires a few annotations.
```
@PiriActivity
public class ExampleActivity extendsAppCompatActivity {
    private static String NUMBER_KEY = "extraNumber";
    private static String STRING_KEY = "extraString";
    private static String DATA_MODEL_KEY = "extraDataModel";
    
    @PiriParam(key = NUMBER_KEY) // the value of key also be the builder method name ("extraNumber")
    private int numberToDisplay;
    
    @PiriParam(key = STRING_KEY)
    private String stringToDisplay;
    
    @PiriParam(key = DATA_MODEL_KEY)
    private DataModel dataModel;
    
    ...
```

Okay cool! But how do we use it?
When you build your project, a class called `ExampleActivityIntentCreator` will be generated:
```
Intent intent = new ExampleActivityIntentCreator(context)
        .extraNumber(number)
        .extraString(string)
        .extraDataModel(dataModel)
        .create();
startActivity(intent);
```

Easy! You can then retrieve your data when `ExampleActivity` is started:
```
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
