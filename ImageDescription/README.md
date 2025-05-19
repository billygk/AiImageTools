

## Testing

To test do something like:

```
curl -X POST \
  http://localhost:8080/api/v1/image/calories-count \
  -F "imageFile=@food_1.jpg"
```



curl -X POST http://localhost:8080/api/v1/image/calories-count -F "imageFile=@food_1.jpg"