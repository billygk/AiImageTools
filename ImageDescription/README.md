

## Testing

To test do something like:

```
curl -X POST \
  http://localhost:8080/api/v1/image/calories-count \
  -F "imageFile=@food_1.jpg"
```

Sample Output for /calories-count

```
curl -X POST http://localhost:8080/api/v1/image/calories-count -F "imageFile=@food_2.jpg"

{
    "response": {
        "Calories": 1200,
        "description": "The image shows a classic fast-food meal consisting of a double cheeseburger, a large order of french fries, and a large soda.  The burger appears to have two beef patties, cheese, lettuce, and tomato on a sesame seed bun. The fries are a standard cut and appear to be in a large portion. The soda is in a large cup, implying a significant volume of sugary beverage. The calorie count is an estimate and can vary based on the specific ingredients and portion sizes."
    }
}
```
