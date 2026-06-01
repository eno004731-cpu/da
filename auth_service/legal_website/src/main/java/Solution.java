// import java.util.Arrays;

// public class Solution {
//     public int missingNumber(int[] nums) {
//        // Напишите здесь свой код
//        if (nums.length ==1) {
//         if (nums[0]>0) {
//             return nums[0]-1;
//         }
//         if(nums[0]==0){
//             return 1;
//         }
//        }
//        if(nums.length ==2){
//         if(nums[1] <1){
//             return nums[1]+1;
//         }
//         else {
//             return nums[1]-1;
//         }
//        }
//        int countNumber = 0;
//        Arrays.sort(nums); 
//        for(int i = 0; i < nums.length-1; i++){
//         int da = nums[i];
//         int net = nums[i+1];
//         if(da - net == -1){
//             countNumber++;
//         }
//         else{
//             countNumber++;
//             System.out.println(countNumber);
//             break;
//         }
//        }
//        System.out.println(countNumber);
//       return countNumber;
//     }
// }
